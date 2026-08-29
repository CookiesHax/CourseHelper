#include <jni.h>
#include <opencv2/opencv.hpp>
#include <vector>
#include <android/log.h>

#define TAG "CaptchaSolverNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

using namespace cv;

extern "C"
JNIEXPORT jint JNICALL
Java_com_cookieshax_coursehelper_feature_checkin_model_CaptchaSolver_nativeCalculateOffset(
        JNIEnv *env,
        jobject thiz,
        jbyteArray bg_data,
        jbyteArray slice_data) {

    // 将 byte 数组转换为 Mat
    jsize bg_len = env->GetArrayLength(bg_data);
    jbyte *bg_bytes = env->GetByteArrayElements(bg_data, nullptr);
    std::vector<uchar> bg_vec(bg_bytes, bg_bytes + bg_len);
    Mat bg_mat = imdecode(bg_vec, IMREAD_COLOR);
    env->ReleaseByteArrayElements(bg_data, bg_bytes, JNI_ABORT);

    jsize slice_len = env->GetArrayLength(slice_data);
    jbyte *slice_bytes = env->GetByteArrayElements(slice_data, nullptr);
    std::vector<uchar> slice_vec(slice_bytes, slice_bytes + slice_len);
    Mat slice_mat = imdecode(slice_vec, IMREAD_COLOR);
    env->ReleaseByteArrayElements(slice_data, slice_bytes, JNI_ABORT);

    if (bg_mat.empty() || slice_mat.empty()) {
        LOGD("Failed to decode images");
        return -1;
    }

    // 转灰度
    Mat bg_gray, slice_gray;
    cvtColor(bg_mat, bg_gray, COLOR_BGR2GRAY);
    cvtColor(slice_mat, slice_gray, COLOR_BGR2GRAY);

    // 获取滑块有效范围 (getCropBounds)
    Mat row_max;
    reduce(slice_gray, row_max, 1, REDUCE_MAX);
    int ymin = 0, ymax = slice_gray.rows - 1;
    for (int i = 0; i < row_max.rows; ++i) {
        if (row_max.at<uchar>(i, 0) > 0) {
            ymin = i;
            break;
        }
    }
    for (int i = row_max.rows - 1; i >= 0; --i) {
        if (row_max.at<uchar>(i, 0) > 0) {
            ymax = i;
            break;
        }
    }

    // 裁剪
    int crop_height = ymax - ymin + 1;
    if (crop_height <= 0) return -1;

    Mat slice_cut = slice_gray(Rect(0, ymin, slice_gray.cols, crop_height));
    Mat bg_cut = bg_gray(Rect(0, ymin, bg_gray.cols, crop_height));

    // 边缘检测
    Mat bg_edge, slice_edge;
    Canny(bg_cut, bg_edge, 100, 200);
    Canny(slice_cut, slice_edge, 100, 200);

    // 模板匹配
    Mat res;
    matchTemplate(bg_edge, slice_edge, res, TM_CCOEFF_NORMED);

    // 获取最佳位置
    double min_val, max_val;
    Point min_loc, max_loc;
    minMaxLoc(res, &min_val, &max_val, &min_loc, &max_loc);

    int x_offset = max_loc.x;
    LOGD("Native calculated offset: %d", x_offset);

    return (jint) x_offset;
}
