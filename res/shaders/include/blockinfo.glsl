
#define BLOCK_ID(blockinfo) (blockinfo.y&0xFFFu)
#define BLOCK_RENDERPASS(blockinfo) float((blockinfo.y&0xF000u)>>12u)
#define BLOCK_TEX_SLOT(blockinfo) float(blockinfo.x)
#define BLOCK_FACEDIR(blockinfo) (blockinfo.w&0x7u)
#define BLOCK_VERTDIR(blockinfo) ((blockinfo.w >> 3u) & 0x3Fu)
#define BLOCK_AO_IDX_0(blockinfo) (in_blockinfo.z)&AO_MASK
#define BLOCK_AO_IDX_1(blockinfo) (in_blockinfo.z>>2u)&AO_MASK
#define BLOCK_AO_IDX_2(blockinfo) (in_blockinfo.z>>4u)&AO_MASK
#define BLOCK_AO_IDX_3(blockinfo) (in_blockinfo.z>>6u)&AO_MASK
#pragma define "IS_SKY"
#pragma define "IS_WATER"
#pragma define "IS_LEAVES"
#pragma define "IS_LIGHT"
#pragma define "IS_WAVING_VERTEX"
#define IS_ILLUM(renderpass) float(renderpass==4)
#define IS_BACKFACE(renderpass) float(renderpass==3)

#define ENCODE_RENDERPASS(renderpass) ((uint(renderpass)&0xFu)<<12u)
