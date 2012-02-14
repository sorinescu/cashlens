#include "yuv2rgb/yuv2rgb.h"
#include "com_udesign_cashlens_CashLensUtils.h"

extern "C"
void nv21_2_rgb565(uint8_t  *dst_ptr,
               const uint8_t  *y_ptr,
               const uint8_t  *u_ptr,
               const uint8_t  *v_ptr,
                     int32_t   width,
                     int32_t   height,
                     int32_t   y_span,
                     int32_t   uv_span,
                     int32_t   dst_span,
               const uint32_t *tables,
                     int32_t   dither);

JNIEXPORT void JNICALL Java_com_udesign_cashlens_CashLensUtils_nv21ToRGB565
  (JNIEnv *, jclass, jbyteArray yuvs, jbyteArray rgbs, jint width, jint height)
{
	nv21_2_rgb565((uint8_t *)rgbs, (uint8_t *)yuvs, (uint8_t *)yuvs + width*height, (uint8_t *)0,
			(int32_t)width, (int32_t)height,
			(int32_t)width, (int32_t)width/2, (int32_t)width*2,
			(const uint32_t *)yuv2bgr565_table, 0);
}
