package nidefawl.qubes.vulkan;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.GameMath;

public class VkMemoryManager {
    static VkMemoryAllocateInfo allocInfo;//not doing any funny thread allocation (yet)
    static VkMemoryRequirements memReqs;//not doing any funny thread allocation (yet)
    static PointerBuffer ptrBuf;
    final static boolean DEBUG_MEM_ALLOC = false;
    private static final long MAX_FRAGMENTATION_SIZE = 16*1024;
    static final HashMap<Long, MemoryChunk> memoryBindings = new HashMap<>();
    public class VulkanMemoryType {
        public VulkanMemoryType(VkMemoryType memoryTypes, int idx) {
            this.idx = idx;
            this.flags = memoryTypes.propertyFlags();
            this.heap = memoryTypes.heapIndex();
        }
        int idx;
        int flags;
        int heap;
    }
    public class MemoryChunk {
        public final MemoryBlock block;
        public long offset;
        public long size;
        public long align;
        private String tag;
        public MemoryChunk(MemoryBlock block, long offset, long size, long align) {
            this.block = block;
            this.offset = offset;
            this.size = size;
            this.align = align;
        }
        public long map() {
            if (block.isMapped) {
                throw new GameLogicError("cannot map twice");
            }
            block.isMapped = true;
            int err = vkMapMemory(block.device, block.memory, this.offset, this.size, 0, ptrBuf);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkAllocateMemory failed: " + VulkanErr.toString(err));
            }
            return ptrBuf.get(0);
        }
        public void unmap() {
            vkUnmapMemory(block.device, block.memory);
            block.isMapped = false;
        }
        
        @Override
        public String toString() {
            return "memChunk[0x"+Long.toHexString(this.offset)+",size=0x"+Long.toHexString(this.size)+",align="+this.align + (this.tag != null?",tag="+this.tag:"")+"]";
        }
        public void tag(String tag) {
            this.tag = tag;
        }
    }
    public class MemoryBlock {
        public boolean isMapped;
        public VkDevice device;
        public int flags;
        public LongBuffer memPointer;
        public long memory;
        public long blockSize;
        public long offset;
        ArrayList<MemoryChunk> unused = new ArrayList<>();
        ArrayList<MemoryChunk> list = new ArrayList<>();
        private VulkanMemoryType memType;
        private boolean isOptimalOnly;
        public MemoryBlock(boolean optimal) {
            this.isOptimalOnly = optimal;
        }
        long getLeft() {
            return this.blockSize-offset;
        }

        public void allocate(VkDevice device, VulkanMemoryType memType, long allocationBlockSize) {
            if (flags != 0) {
                throw new GameLogicError("Reallocing, dummy");
            }
            System.out.println("Allocating block of "+allocationBlockSize+" of memtype "+memType.idx+", flags "+memType.flags);
            this.device = device;
            this.memType = memType;
            this.offset = 0;
            this.blockSize = allocationBlockSize;
            this.flags = memType.flags;
            allocInfo.allocationSize(allocationBlockSize);
            allocInfo.memoryTypeIndex(memType.idx);
            this.memPointer = memAllocLong(1);
            int err = vkAllocateMemory(device, allocInfo, null, memPointer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkAllocateMemory failed: " + VulkanErr.toString(err));
            }
            this.memory = memPointer.get(0);
        }

        public MemoryChunk allocateChunk(long align, long size) {
            if (DEBUG_MEM_ALLOC) {
                check();
            }
            if (size > this.blockSize) {
                dump();
                throw new GameLogicError("size > this.blockSize");
            }
            if (size > getLeft()) {
                dump();
                throw new GameLogicError("size > getLeft()");
            }
            if ((size & align) != 0) {
                size = (size/align+1)*align;
            }
            while (this.unused.size() > 1) {
                if (DEBUG_MEM_ALLOC) {
                    for (int i = 0; i < unused.size(); i++) {
                        MemoryChunk c = unused.get(i);
                        System.out.println("unused["+i+"] = "+c);
                    }
                }
                boolean merged = false;
                MemoryChunk chunk = unused.get(0);
                for (int i = 1; i < unused.size(); i++) {
                    MemoryChunk chunk2 = unused.get(i);
                    if (chunk2.offset == chunk.offset+chunk.size) {
                        if (DEBUG_MEM_ALLOC) System.out.println("mergin chunk to right: "+chunk+" and "+chunk2+"");
                        chunk.size+=chunk2.size;
                        unused.remove(i--);
                        merged = true;
                    } else if (chunk2.offset+chunk2.size==chunk.offset) {
                        if (DEBUG_MEM_ALLOC) System.out.println("mergin chunk to left: "+chunk+" and "+chunk2+"");
                        chunk.size+=chunk2.size;
                        chunk.offset=chunk2.offset;
                        chunk.align = chunk2.align;
                        unused.remove(i--);
                        merged = true;
                    }
                }
                if (!merged) {
                    break;
                } else {
                    while (this.unused.size() > 0) {
                        boolean freed = false;
                        for (int i = 0; i < unused.size(); i++) {
                            chunk = unused.get(i);
                            if (chunk.offset+chunk.size == this.offset) {
                                if (DEBUG_MEM_ALLOC) System.out.println("freeing "+chunk+" in allocator");
                                this.offset -= chunk.size;
                                unused.remove(i--);
                                freed = true;
                            }
                        }
                        if (!freed) {
                            break;
                        }
                    }

                }
            }
            for (int i = 0; i < unused.size(); i++) {
                MemoryChunk chunk = unused.get(i);
                if (chunk.size >= size && chunk.align == align) {
                    unused.remove(i);
                    list.add(chunk);
                    if (chunk.size-size > MAX_FRAGMENTATION_SIZE) {
                        long newChunkSize = chunk.size-size;
                        long newChunkOffset = chunk.offset+newChunkSize;
                        chunk.size = size;
                        MemoryChunk newChunk = new MemoryChunk(this, newChunkOffset, newChunkSize, chunk.align);
                        unused.add(newChunk);
                        if (DEBUG_MEM_ALLOC) {
                            System.out.println("using splitted chunk entry, buffer now has "+(chunk.size-size)+" extra bytes claimed!");
                        }
                    } else {
                        if (DEBUG_MEM_ALLOC) {
                            System.out.println("using free chunk entry, buffer now has "+(chunk.size-size)+" extra bytes claimed!");
                        }
                    }
                    if (DEBUG_MEM_ALLOC) {
                        check();
                    }
                    return chunk;
                }
            }
            long chunkoffset = this.offset;
            if ((chunkoffset & align) != 0) {
                chunkoffset = (chunkoffset/align+1)*align;
            }

            MemoryChunk chunk = new MemoryChunk(this, chunkoffset, size, align);
            this.offset = chunk.offset+chunk.size;
            list.add(chunk);
            if (DEBUG_MEM_ALLOC) {
                System.out.println("Allocating new chunk of "+size+", align "+align+" of memtype "+memType.idx+", flags "+memType.flags+" = "+chunk);
                check();
            }
            return chunk;
        }
        public void dealloc(MemoryChunk chunk) {
            list.remove(chunk);
            if (DEBUG_MEM_ALLOC) System.out.println("Dealloc chunk "+chunk+", "+chunk.size);
            if (chunk.offset+chunk.size == this.offset) {
                if (DEBUG_MEM_ALLOC) System.out.println("Freeing up, cur offset: "+this.offset+", new offset : "+(this.offset-chunk.size));
                this.offset -= chunk.size;
            } else {
                if (DEBUG_MEM_ALLOC) System.out.println("Adding to unused list (fragmentation)");
                unused.add(chunk);
            }
            if (DEBUG_MEM_ALLOC) {
                check();
            }
        }
        public void check() {
            ArrayList<MemoryChunk> allChunks = new ArrayList<>();
            allChunks.addAll(list);
            allChunks.addAll(unused);
            for (int i = 0; i < list.size(); i++) {
                MemoryChunk c = list.get(i);

                for (int j = 0; j < list.size(); j++) {
                    if (i == j) 
                        continue;
                    MemoryChunk c2 = list.get(j);
                    if (c2.offset>=c.offset+c.size)
                        continue;
                    if (c2.offset+c2.size<=c.offset)
                        continue;
                    throw new GameLogicError("collision");
                }
            }
            
        }
        public void dump() {
            ArrayList<MemoryChunk> allChunks = new ArrayList<>();
            allChunks.addAll(list);
            allChunks.addAll(unused);
            Collections.sort(allChunks, new Comparator<MemoryChunk>() {
                @Override
                public int compare(MemoryChunk o1, MemoryChunk o2) {
                    return Long.compare(o1.offset, o2.offset);
                }
            });
            long lastEnd = 0;
            for (int i = 0; i < allChunks.size(); i++) {
                MemoryChunk c = allChunks.get(i);
                long freeSpace = c.offset-lastEnd;
                if (freeSpace > 0) {
                    System.out.println("Free 0x"+Long.toHexString(lastEnd)+" to 0x"+Long.toHexString(c.offset));
                }
                lastEnd = c.offset+c.size;
                System.out.println(c);
            }
            System.out.println("Free 0x"+Long.toHexString(lastEnd)+" to 0x"+Long.toHexString(this.blockSize)+" = "+((this.blockSize-lastEnd)/MB)+"MB");
            System.out.println("Free 0x"+Long.toHexString(this.offset)+" to 0x"+Long.toHexString(this.blockSize)+" = "+((this.blockSize-this.offset)/MB)+"MB");
            
        }
        
    }
    final static MemoryBlock[] blocks = new MemoryBlock[16];
    private static final long MB = 1024L*1024L;
    private VKContext ctxt;
    private VulkanMemoryType[] memTypes;
    private long deviceLocalHeapSize;
    private long allocationBlockSize;
    
    public static int getMemoryType(VKContext vkContext, int typeBits, int properties) {
        for (int i = 0; i < vkContext.memoryProperties.memoryTypeCount(); i++)
        {
            if ((typeBits & 1) != 0)
            {
                if ((vkContext.memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties)
                {
                    return i;
                }
            }
            typeBits >>= 1;
        }
        throw new AssertionError("Did not find matching memory type");
    }
    
    public VkMemoryManager(VKContext context) {
        this.ctxt = context;
        allocInfo = VkMemoryAllocateInfo.calloc().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        memReqs = VkMemoryRequirements.calloc();
        ptrBuf = memAllocPointer(1);
        this.memTypes = new VulkanMemoryType[context.memoryProperties.memoryTypeCount()];
        for (int i = 0; i < this.memTypes.length; i++) {
            this.memTypes[i] = new VulkanMemoryType(context.memoryProperties.memoryTypes(i), i);
        }
        int nHeaps = context.memoryProperties.memoryHeapCount();
        System.out.println("Device has "+nHeaps+" memory heaps");
        for (int i = 0; i < nHeaps; i++) {
            String heapFlagsStr = "";
            VkMemoryHeap heap = context.memoryProperties.memoryHeaps(i);
            int heapflags = heap.flags();
            for (int j = 0; j < 8; j++) {
                if ((heapflags & (1<<j)) != 0) {
                    if (!heapFlagsStr.isEmpty()) {
                        heapFlagsStr+=",";
                    }
                    if (j==0) {
                        heapFlagsStr+="VK_MEMORY_HEAP_DEVICE_LOCAL_BIT";
                    } else
                    heapFlagsStr+=Integer.toHexString((1<<j));
                }
            }
            if (heapFlagsStr.isEmpty()) {
                heapFlagsStr=Integer.toHexString(heapflags);
            }
            System.out.println("Heap (Size "+(heap.size()/(MB))+"MB) "+i+" has flags "+heapFlagsStr);
            if ((heapflags&VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0) {
                deviceLocalHeapSize = heap.size();
            }
        }
        if (deviceLocalHeapSize <= 0) {
            throw new GameLogicError("No supported memory type present");
        }
        this.allocationBlockSize = Math.max(64*MB, Math.min(1024*MB, deviceLocalHeapSize/32));
        if ((this.allocationBlockSize&0xFFFF) != 0) {
            this.allocationBlockSize = Math.max(64*MB, Math.min(1024*MB, GameMath.nextPowerOf2(allocationBlockSize)));
        }
        System.out.println("allocationBlockSize is "+(this.allocationBlockSize/MB)+"MB 0x"+Long.toHexString(allocationBlockSize));

        int nMemoryTypes = context.memoryProperties.memoryTypeCount();
        System.out.println("Device has "+nMemoryTypes+" memory types");
        for (int i = 0; i < nMemoryTypes; i++) {
            String memtypeflagsStr = "";
            VkMemoryType memType = context.memoryProperties.memoryTypes(i);
            int memtypeflags = memType.propertyFlags();
            for (int j = 0; j < 8; j++) {
                if ((memtypeflags & (1<<j)) != 0) {
                    if (!memtypeflagsStr.isEmpty()) {
                        memtypeflagsStr+=",";
                    }
                    if (j==0) {
                        memtypeflagsStr+="VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT";
                    } else if (j==1) {
                        memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT";
                    } else if (j==2) {
                        memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_COHERENT_BIT";
                    } else if (j==3) {
                        memtypeflagsStr+="VK_MEMORY_PROPERTY_HOST_CACHED_BIT";
                    } else if (j==4) {
                        memtypeflagsStr+="VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT";
                    } else 
                    memtypeflagsStr+=Integer.toHexString((1<<j));
                }
            }
            memtypeflagsStr="("+Integer.toBinaryString(memtypeflags)+")"+memtypeflagsStr;
            System.out.println("Type "+i+" (Heap "+memType.heapIndex()+") has flags "+memtypeflagsStr);
        }
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, true);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, false);
    }

    private void allocateBlock(VkDevice device, int properties, boolean optimal) {
        VulkanMemoryType mem = getMemTypeByFlags(properties);
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == null) {
                blocks[i] = new MemoryBlock(optimal);
                blocks[i].allocate(device, mem, this.allocationBlockSize);
                return;
            }
        }
    }

    //use a map if used frequently
    private VulkanMemoryType getMemTypeByFlags(int properties) {
        for (int i = 0; i < memTypes.length; i++)
        {
            if ((memTypes[i].flags & properties) == properties)
            {
                return memTypes[i];
            }
        }
        throw new GameLogicError("No supported memory type present");
    }
    //use a map if used frequently
    private MemoryBlock getMemBlockByFlags(int supportedTypes, int requiredFlags, boolean isOptimalImageBlock) {
        for (int i = 0; i < blocks.length; i++) {
            
            if (blocks[i] != null) {
                if (((supportedTypes>>blocks[i].memType.idx)&1) != 0) {
                    if ((blocks[i].flags & requiredFlags) == requiredFlags && blocks[i].isOptimalOnly == isOptimalImageBlock) {
                        return blocks[i];
                    }
                }
            }
        }
        throw new GameLogicError("No supported memory type present");
    }

    public MemoryChunk allocateImageMemory(long image, int properties, int debug) {
        vkGetImageMemoryRequirements(ctxt.device, image, memReqs);
        MemoryBlock block = getMemBlockByFlags(memReqs.memoryTypeBits(), properties, true);
        long align = Math.max(memReqs.alignment(), this.ctxt.limits.bufferImageGranularity());
        long size = memReqs.size();
        MemoryChunk chunk = block.allocateChunk(align, size);
        if (DEBUG_MEM_ALLOC) System.out.println("image "+image+","+debug+" requires "+(size)+" bytes");
        int err = vkBindImageMemory(ctxt.device, image, chunk.block.memory, chunk.offset);
        if (err != VK_SUCCESS) {
            throw new AssertionError("vkBindImageMemory failed: " + VulkanErr.toString(err));
        }
        memoryBindings.put(image, chunk);
        return chunk;
    }

    public void releaseImageMemory(long image) {
        release(image);
    }
    public void releaseBufferMemory(long buffer) {
        release(buffer);
    }

    private void release(long binding) {
        MemoryChunk chunk = memoryBindings.get(binding);
        if (chunk == null) {
            throw new GameLogicError("not bound");
        }
        chunk.block.dealloc(chunk);
    }

    public MemoryChunk allocateBufferMemory(long buffer, int properties) {
        return allocateBufferMemory(buffer, properties, null);
    }
    public MemoryChunk allocateBufferMemory(long buffer, int properties, String tag) {
        vkGetBufferMemoryRequirements(ctxt.device, buffer, memReqs);
        MemoryBlock block = getMemBlockByFlags(memReqs.memoryTypeBits(), properties, false);
        long align = memReqs.alignment();
        long size = memReqs.size();
        if (DEBUG_MEM_ALLOC) System.out.println("buffer "+(tag!=null?tag:buffer)+" requires "+(size)+" bytes");
        MemoryChunk chunk = block.allocateChunk(align, size);
        chunk.tag(tag);
        int err = vkBindBufferMemory(ctxt.device, buffer, chunk.block.memory, chunk.offset);
        if (err != VK_SUCCESS) {
            throw new AssertionError("vkBindBufferMemoryvkBindBufferMemory failed: " + VulkanErr.toString(err));
        }
        memoryBindings.put(buffer, chunk);
        return chunk;
    }

    public void shudown() {
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] != null) {
                MemoryUtil.memFree(blocks[i].memPointer);
                vkFreeMemory(ctxt.device, blocks[i].memory, null);
                blocks[i] = null;
            }
        }
        memoryBindings.clear();
        allocInfo.free();
        memReqs.free();
        MemoryUtil.memFree(ptrBuf);
    }
}
