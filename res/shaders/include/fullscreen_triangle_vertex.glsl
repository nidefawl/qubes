
    vec2 pos;
    pos.x = float(gl_VertexID & 1)*2.0;
    pos.y = float(gl_VertexID & 2);
    pass_texcoord.st = pos;
    gl_Position = vec4(pos * 2.0 - 1.0, 0, 1);