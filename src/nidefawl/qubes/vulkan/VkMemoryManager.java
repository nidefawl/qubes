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
    private static VkMemoryAllocateInfo allocInfo;//not doing any funny thread allocation (yet)
    private static VkMemoryRequirements memReqs;
    private static VkMappedMemoryRange flushRanges;
    private static PointerBuffer ptrBuf;

    public static void allocStatic() {
        allocInfo = VkMemoryAllocateInfo.calloc().sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        flushRanges = VkMappedMemoryRange.calloc().sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE);
        flushRanges.pNext(0L);
        memReqs = VkMemoryRequirements.calloc();
        ptrBuf = memAllocPointer(1);
    }
    public static void destroyStatic() {
        memoryBindings.clear();
        allocInfo.free();
        memReqs.free();
        MemoryUtil.memFree(ptrBuf);
    }

    static final boolean DEBUG_MEM_ALLOC = false;
    static boolean MEM_ALLOC_CRASH = false;
    private static final long MAX_FRAGMENTATION_SIZE = 16*1024;
    public static long SIZE_BLOCK_ALLOCD;
    public static long SIZE_CHUNK_ALLOCD;
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
            return mapRange(0, this.size);
        }
        public long mapRange(long mapOffset, long mapSize) {
            if (block.isMapped) {
                throw new GameLogicError("cannot map twice");
            }
            block.isMapped = true;
//            System.out.println("MAP "+size);
            int err = vkMapMemory(block.device, block.memory, this.offset+mapOffset, mapSize, 0, ptrBuf);
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkAllocateMemory failed: " + VulkanErr.toString(err));
            }
//            flushRanges.offset(this.offset+mapOffset);
//            flushRanges.size(mapSize);
            return ptrBuf.get(0);
        }
        public void unmap() {
//            System.err.println(this.size+","+this.offset+","+(this.offset+size));
//            flushRanges.memory(block.memory);
//            flushRanges.offset(this.offset);
//            flushRanges.size(this.size);
//            vkFlushMappedMemoryRanges(block.device, flushRanges);
//            flushRanges.memory(block.memory);
//            flushRanges.offset(block.offset);
//            flushRanges.size(this.size);
//            vkFlushMappedMemoryRanges(block.device, flushRanges);
            vkUnmapMemory(block.device, block.memory);
            block.isMapped = false;
        }
        
        @Override
        public String toString() {
            return "memChunk[0x"+Long.toHexString(this.offset)+",size=0x"+Long.toHexString(this.size)+" "+String.format("%.2f", (this.size/(float)MB))+",align="+this.align + (this.tag != null?",tag="+this.tag:"")+"]";
        }
        public void tag(String tag) {
            this.tag = tag;
        }
    }
    public class MemoryBlock {
        public boolean isMapped;
        public VkDevice device;
        public int flags;
        public LongBuffer memPointer = null;
        public long memory = VK_NULL_HANDLE;
        public long blockSize;
        public long offset;
        ArrayList<MemoryChunk> unused = new ArrayList<>();
        ArrayList<MemoryChunk> list = new ArrayList<>();
        private VulkanMemoryType memType;
        private final boolean isImageMemory;
        private final boolean shared;
        public MemoryBlock(boolean shared, boolean optimal) {
            this.shared = shared;
            this.isImageMemory = optimal;
        }
        long getLeft() {
            return this.blockSize-offset;
        }
        boolean freed=false;
        public void freeBlock(VkDevice device) {
            if (freed) {
                throw new GameLogicError("Double free!");
            }
            if (this.memory != VK_NULL_HANDLE) {
                freed = true;
                MemoryUtil.memFree(this.memPointer);
                vkFreeMemory(device, this.memory, null);
                this.memory = VK_NULL_HANDLE;
                this.memPointer = null;
                SIZE_BLOCK_ALLOCD-=this.blockSize;
            }
        }
        public void allocateBlock(VkDevice device, VulkanMemoryType memType, long allocationBlockSize) {
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
            SIZE_BLOCK_ALLOCD+=this.blockSize;
        }

        public MemoryChunk allocateChunk(long align, long size) {
            if (DEBUG_MEM_ALLOC) {
                check();
            }
            if (size > this.blockSize) {
                dump();
                throw new GameLogicError("size > this.blockSize");
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
                        long newChunkOffset = chunk.offset+size;
                        if (newChunkSize > chunk.align) {
                            long shiftAlign = (chunk.align-(newChunkOffset%chunk.align))%chunk.align;
//                            System.out.println("shifting by "+shiftAlign+" so newchunkoffset "+newChunkOffset+" stis on "+(newChunkOffset+shiftAlign)+" and offers alignement "+chunk.align);
                            if (shiftAlign != 0&&newChunkSize>shiftAlign) {
                                newChunkOffset += shiftAlign;
                                newChunkSize -= shiftAlign;
                            }
                            int alignRet = -1;
                            for (int o = 24; o >= 0; o--) {
                                if (((newChunkOffset)&((1<<o)-1))==0) {
                                    alignRet = 1<<o;
                                    break;
                                }
                            }
                            if (chunk.align > alignRet) {
                                throw new IllegalStateException("Alignment fucked up "+chunk.align+" is not "+alignRet);
                            }
                        } else {
                            chunk.align = 0;
                        }
                        chunk.size = size;
                        MemoryChunk newChunk = new MemoryChunk(this, newChunkOffset, newChunkSize, chunk.align);
                        newChunk.tag = "free_split";
                        unused.add(newChunk);
                        if (DEBUG_MEM_ALLOC) {
                            System.out.println("using splitted chunk entry, buffer now has "+(chunk.size-size)+" extra bytes claimed!");
                            System.out.println("split is "+chunk);
                            int alignRet = -1;
                            for (int o = 24; o >= 0; o--) {
                                if (((chunk.offset)&((1<<o)-1))==0) {
                                    alignRet = 1<<o;
                                    break;
                                }
                            }
                            System.out.println("actual alignement of split is "+alignRet+", req was "+align+" chunk stored is "+chunk.align);

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
            if (size > getLeft()) {
                dump();
                throw new GameLogicError("size > getLeft()");
            }
            long chunkoffset = this.offset;
            if ((chunkoffset % align) != 0) {
                if (DEBUG_MEM_ALLOC) {
                    System.out.println("aligning to "+align+", offsetIn "+offset +", offsetOut "+((chunkoffset/align+1)*align));
                }
                chunkoffset = (chunkoffset/align+1)*align;
            }
            if (this.blockSize-chunkoffset < size) {
                throw new GameLogicError("aligned size > getLeft()");
            }

            MemoryChunk chunk = new MemoryChunk(this, chunkoffset, size, align);
            this.offset = chunk.offset+chunk.size;
            list.add(chunk);
            if (DEBUG_MEM_ALLOC) {
                System.out.println("Allocating new chunk of "+size+", align "+align+" of memtype "+memType.idx+", flags "+memType.flags+" = "+chunk);
                if (chunk.offset%align != 0) {
                    throw new GameLogicError("memoryOffset 0x"+Long.toHexString(chunk.offset)+" is not aligned to 0x"+Long.toHexString(align));
                }
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
                chunk.tag = "free";
                unused.add(chunk);
            }
            if (DEBUG_MEM_ALLOC) {
                check();
            }
        }
        public void check() {
            if (MEM_ALLOC_CRASH) return;
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
                    dump();
                    MEM_ALLOC_CRASH = true;
                    throw new GameLogicError("collision between 0x"+Long.toHexString(c.offset)+" and 0x"+Long.toHexString(c2.offset));
                }
            }
            
        }
        public long getAllocSum() {
            ArrayList<MemoryChunk> allChunks = new ArrayList<>();
            allChunks.addAll(list);
            allChunks.addAll(unused);
            long size = 0L;
            for (int i = 0; i < allChunks.size(); i++) {
                size += allChunks.get(i).size;
            }
            return size;
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
            if (this.blockSize-lastEnd > 0)
            System.out.println("Free 0x"+Long.toHexString(lastEnd)+" to 0x"+Long.toHexString(this.blockSize)+" = "+((this.blockSize-lastEnd)/MB)+"MB");
            if (this.blockSize-this.offset > 0 && this.offset != lastEnd)
            System.out.println("Free 0x"+Long.toHexString(this.offset)+" to 0x"+Long.toHexString(this.blockSize)+" = "+((this.blockSize-this.offset)/MB)+"MB");
            
        }
        public boolean canAlloc(long size, long align) {
            if (size > this.blockSize) {
                dump();
                throw new GameLogicError("size > this.blockSize "+size+" > "+this.blockSize);
            }
            if ((size & align) != 0) {
                size = (size/align+1)*align;
            }
            if (getLeft() >= size) {
                long chunkoffset = this.offset;
                if ((chunkoffset & align) != 0) {
                    chunkoffset = (chunkoffset/align+1)*align;
                }
                if (this.blockSize-chunkoffset >= size) {
                    return true;
                }
            }
            for (int i = 0; i < unused.size(); i++) {
                MemoryChunk chunk = unused.get(i);
                if (chunk.size >= size && chunk.align % align == 0) {
                    return true;
                }
            }
            return false;
        }
        
    }
    final MemoryBlock[] blocks = new MemoryBlock[16];
    public static final long MB = 1024L*1024L;
    private VKContext ctxt;
    private VulkanMemoryType[] memTypes;
    private long deviceLocalHeapSize;
    private long allocationBlockSize;
    private ArrayList<MemoryBlock> allBlocks = new ArrayList<>();
    private ArrayList<MemoryBlock> unshared = new ArrayList<>();
    public boolean wholeBlock;
    
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
        this.allocationBlockSize = Math.max(128*MB, Math.min(1024*MB, deviceLocalHeapSize/32));
        if ((this.allocationBlockSize&0xFFFF) != 0) {
            this.allocationBlockSize = Math.max(128*MB, Math.min(1024*MB, GameMath.nextPowerOf2(allocationBlockSize)));
        }
        System.out.println("allocationBlockSize is "+(this.allocationBlockSize/MB)+"MB 0x"+Long.toHexString(allocationBlockSize));

        int nMemoryTypes = context.memoryProperties.memoryTypeCount();
        System.out.println("Device has "+nMemoryTypes+" memory types");
        for (int i = 0; i < nMemoryTypes; i++) {
            VkMemoryType memType = context.memoryProperties.memoryTypes(i);
            int memtypeflags = memType.propertyFlags();
            
            String memtypeflagsStr = VulkanErr.memFlagsToStr(memtypeflags);
            memtypeflagsStr="("+Integer.toBinaryString(memtypeflags)+")"+memtypeflagsStr;
            System.out.println("Type "+i+" (Heap "+memType.heapIndex()+") has flags "+memtypeflagsStr);
        }
        /*for (int i = 0; i < 5; i++) {
            allocateBlock(context.device, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, false);
        }
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, true);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, true);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
        allocateBlock(context.device, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, false);
		*/
    }
    public void dump() {
        for (int i = 0; i < allBlocks.size(); i++) {
            MemoryBlock block = allBlocks.get(i);
            long l = block.getAllocSum();
            System.out.println("--- Block["+i+"], device idx "+block.memType.idx+", "+VulkanErr.memFlagsToStr(block.flags)+", Usage "+(l/MB)+"/"+(block.blockSize/MB)+"MB");
//            block.dump();
        }
    }

    private MemoryBlock allocateBlock(VkDevice device, int supportedTypes, int properties, boolean optimal) {
        VulkanMemoryType mem = getMemTypeByFlagsAndType(supportedTypes, properties);
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == null) {
                blocks[i] = new MemoryBlock(true, optimal);
                blocks[i].allocateBlock(device, mem, this.allocationBlockSize);
                allBlocks.add(blocks[i]);
                return blocks[i];
            }
        }
        throw new GameLogicError("No free memory block slot");
    }

    //use a map if used frequently
    private VulkanMemoryType getMemTypeByFlagsAndType(int supportedTypes, int properties) {
        for (int i = 0; i < memTypes.length; i++)
        {
            if (((supportedTypes>>i)&1) != 0) {
                if ((memTypes[i].flags & properties) == properties)
                {
                    return memTypes[i];
                }
            }
        }
        throw new GameLogicError("No supported memory type present");
    }

    //use a map if used frequently
    private MemoryBlock getMemBlockByFlags(int supportedTypes, int requiredFlags, boolean imageMemory, long size, long align) {
        for (int i = 0; i < blocks.length; i++) {
            
            if (blocks[i] != null) {
                if (((supportedTypes>>blocks[i].memType.idx)&1) != 0) {
                    if ((blocks[i].flags & requiredFlags) == requiredFlags && blocks[i].isImageMemory == imageMemory) {
                        if (blocks[i].canAlloc(size, align))
                            return blocks[i];
                    }
                }
            }
        }
      	return allocateBlock(this.ctxt.device, supportedTypes, requiredFlags, imageMemory);
        
        //throw new GameLogicError("No supported memory type present. requested size "+size+","+supportedTypes+","+requiredFlags+","+imageMemory+","+align);
    }

    public void releaseImageMemory(long image) {
        release(image);
    }
    public void releaseBufferMemory(long buffer) {
        release(buffer);
    }

    private void release(long binding) {
        MemoryChunk chunk = memoryBindings.remove(binding);
        if (chunk == null) {
            throw new GameLogicError("not bound");
        }
        SIZE_CHUNK_ALLOCD-=chunk.size;
        chunk.block.dealloc(chunk);
        if (!chunk.block.shared) {
            chunk.block.freeBlock(ctxt.device);
            unshared.remove(chunk.block);
            allBlocks.remove(chunk.block);
        }
    }
    public MemoryChunk allocChunk(int supportedTypes, long size, long align, int properties, boolean image, Object tag) {
        MemoryBlock block;
        long alignSize = size;
        if ((alignSize & align) != 0) {
            alignSize = (alignSize/align+1)*align;
        }
                
        if (alignSize > this.allocationBlockSize/2||this.wholeBlock) {
            System.out.println("Allocating full block for request of size "+alignSize);
            VulkanMemoryType mem = getMemTypeByFlagsAndType(supportedTypes, properties);
            block = new MemoryBlock(false, image);
            block.allocateBlock(ctxt.device, mem, alignSize);
            unshared.add(block);
            allBlocks.add(block);
        } else {
            block = getMemBlockByFlags(supportedTypes, properties, image, alignSize, align);
        }
        if (DEBUG_MEM_ALLOC) System.out.println("alloc "+(tag!=null?tag:"")+" requires "+(alignSize)+" bytes");
        MemoryChunk chunk = block.allocateChunk(align, alignSize);
        if (DEBUG_MEM_ALLOC) System.out.println("alloc "+(tag!=null?tag:"")+" got chunk "+(chunk));
        SIZE_CHUNK_ALLOCD+=chunk.size;
        return chunk;
    }
    public MemoryChunk allocateBufferMemory(long buffer, int properties) {
        return allocateBufferMemory(buffer, properties, null);
    }
    public MemoryChunk allocateBufferMemory(long buffer, int properties, String tag) {
        vkGetBufferMemoryRequirements(ctxt.device, buffer, memReqs);
        long align = memReqs.alignment();
        if (align < 16384)
            align = 16384;
        long size = memReqs.size();
        MemoryChunk chunk = allocChunk(memReqs.memoryTypeBits(), size, align, properties, false, tag);
        chunk.tag(tag);
        int alignRet = -1;
        for (int i = 24; i >= 0; i--) {
            if (((chunk.offset)&((1<<i)-1))==0) {
                alignRet = 1<<i;
                break;
            }
        }
//        System.out.println("ALIGN REQ "+align+", ALIGN RETURNED "+alignRet+", chunk/block align "+chunk.align);
        if (alignRet < align) {
            throw new AssertionError("INVALID ALIGNMENT");
        }
        int err = vkBindBufferMemory(ctxt.device, buffer, chunk.block.memory, chunk.offset);
        if (err != VK_SUCCESS) {
            throw new AssertionError("vkBindBufferMemoryvkBindBufferMemory failed: " + VulkanErr.toString(err));
        }
        MemoryChunk prev = memoryBindings.put(buffer, chunk);
        if (prev != null) {
            throw new GameLogicError("prev binding not null!");
        }
        return chunk;
    }

    public MemoryChunk allocateImageMemory(long image, int properties, int debug) {

        vkGetImageMemoryRequirements(ctxt.device, image, memReqs);
        long align = Math.max(memReqs.alignment(), this.ctxt.limits.bufferImageGranularity());
        long size = memReqs.size();
        MemoryChunk chunk = allocChunk(memReqs.memoryTypeBits(), size, align, properties, true, null);
        if (DEBUG_MEM_ALLOC) System.out.println("image "+image+","+debug+" requires "+(size)+" bytes");
        int err = vkBindImageMemory(ctxt.device, image, chunk.block.memory, chunk.offset);
        if (err != VK_SUCCESS) {
            System.err.println("Align "+Long.toHexString(align));
            throw new AssertionError("vkBindImageMemory failed: " + VulkanErr.toString(err));
        }
        MemoryChunk prev = memoryBindings.put(image, chunk);
        if (prev != null) {
            throw new GameLogicError("prev binding not null!");
        }
        return chunk;
    }

    public void shudown() {
        for (int i = 0; i < allBlocks.size(); i++) {
            allBlocks.get(i).freeBlock(ctxt.device);
        }
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = null;
        }
        unshared.clear();
        allBlocks.clear();
    }
}
