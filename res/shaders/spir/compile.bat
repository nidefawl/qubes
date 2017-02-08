@echo off
set PATH=C:\VulkanSDK\1.0.39.1\bin\;%PATH%
glslangValidator -G triangle.vert -o triangle.vert.spv
glslangValidator -G triangle.frag -o triangle.frag.spv