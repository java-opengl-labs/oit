//--------------------------------------------------------------------------------------
// Order Independent Transparency with Depth Peeling
//
// Author: Louis Bavoil
// Email: sdkfeedback@nvidia.com
//
// Copyright (c) NVIDIA Corporation. All rights reserved.
//--------------------------------------------------------------------------------------

#version 330

uniform sampler2DRect colorTex;

uniform vec3 backgroundColor;

out vec4 outputColor;

void main(void)
{
    vec4 frontColor = texture(colorTex, gl_FragCoord.xy);
    outputColor.rgb = frontColor.rgb + backgroundColor * frontColor.a;
    outputColor.a = 1;
}
