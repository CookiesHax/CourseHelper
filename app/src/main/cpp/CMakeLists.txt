cmake_minimum_required(VERSION 3.22.1)

project("coursehelper")

find_package(OpenCV REQUIRED)

add_library(captcha_solver SHARED
        captcha_solver.cpp
)

target_link_libraries(captcha_solver
        ${OpenCV_LIBS}
        android
        log
)
