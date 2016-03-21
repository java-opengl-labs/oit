//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 400

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;

uniform mat4 modelToWorld;

layout(std140) uniform vpMatrixes  {

    mat4 worldToCamera;
    mat4 cameraToClip;
};

smooth out vec3 interpolated;

vec3 ShadeVertex();

void main(void) {

    gl_Position = cameraToClip * worldToCamera * modelToWorld * vec4(position, 1.0);
    interpolated = ShadeVertex();
}