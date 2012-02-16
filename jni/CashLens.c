#include <jni.h>
#include "libyuv/cpu_id.h"
#include "libyuv/planar_functions.h"
#include "com_udesign_cashlens_CashLensUtils.h"

JNIEXPORT void JNICALL APIENTRY Java_com_udesign_cashlens_CashLensUtils_initLibYUV
  (JNIEnv *env, jclass cls)
{
	// First time this function is run also detects the processor capabilities
	TestCpuFlag(0);
}

JNIEXPORT void JNICALL APIENTRY Java_com_udesign_cashlens_CashLensUtils_nv21ToRGB565
  (JNIEnv *env, jclass cls, jbyteArray yuvsArray, jbyteArray rgbsArray, jint width, jint height)
{
	jbyte *yuvs;
    jbyte *rgbs;
    yuvs = (*env)->GetByteArrayElements(env, yuvsArray, JNI_FALSE);
    rgbs = (*env)->GetByteArrayElements(env, rgbsArray, JNI_FALSE);
    //see http://java.sun.com/docs/books/jni/html/functions.html#100868
    //If isCopy is not NULL, then *isCopy is set to JNI_TRUE if a copy is made; if no copy is made, it is set to JNI_FALSE.

    // Sacrifice the top left pixel, but offset the UV array by 1 pixel to "convert" NV12 to NV21
    NV12ToRGB565(yuvs, width, yuvs + width*height - 1, width, rgbs, width*2, width, height);

    //release arrays:
    (*env)->ReleaseByteArrayElements(env, yuvsArray, yuvs, 0);
    (*env)->ReleaseByteArrayElements(env, rgbsArray, rgbs, 0);
}
