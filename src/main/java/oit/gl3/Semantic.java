/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3;

import oit.gl4.*;

/**
 *
 * @author gbarbieri
 */
public class Semantic {

    public static class Attr {

        public static final int POSITION = 0;
        public static final int NORMAL = 1;
        public static final int COLOR = 3;
        public static final int TEXCOORD = 4;
        public static final int DRAW_ID = 5;
    }

    public static class Buffer {

        public static final int STATIC = 0;
        public static final int DYNAMIC = 1;
    }

    public static class Frag {

        public static final int COLOR = 0;
        public static final int RED = 0;
        public static final int GREEN = 1;
        public static final int BLUE = 2;
        public static final int ALPHA = 0;
        public static final int SUM_COLOR = 0;
        public static final int SUM_WEIGHT = 1;
    }

    public static class Image {

        public static final int DIFFUSE = 0;
        public static final int PICKING = 1;
    }

    public static class Object {

        public static final int VAO = 0;
        public static final int VBO = 1;
        public static final int IBO = 2;
        public static final int TEXTURE = 3;
        public static final int SAMPLER = 4;
        public static final int SIZE = 5;
    }

    public static class Renderbuffer {

        public static final int DEPTH = 0;
        public static final int COLOR0 = 1;
    }

    public static class Sampler {

        public static final int OPAQUE_DEPTH = 2;
        public static final int OPAQUE_COLOR = 3;
        public static final int DEPTH = 0;
        public static final int TEMP = 0;
        public static final int COLOR = 0;
        /**
         * Dual Depth Peeling.
         */
        public static final int FRONT_BLENDER = 1;
        public static final int BACK_TEMP = 0;
        /**
         * Weighted Average.
         */
        public static final int SUM_COLOR = 0;
        public static final int COUNT = 1;
        /**
         * Weighted Blended.
         */
        public static final int WEIGHTED_SUM = 0;
        public static final int TRANSM_PRODUCT = 1;
    }

    public static class Storage {

        public static final int VERTEX = 0;
    }

    public static class Uniform {

        public static final int TRANSFORM0 = 0;
        public static final int TRANSFORM1 = 1;
        public static final int TRANSFORM2 = 2;
        public static final int PARAMETERS = 3;
        public static final int BACKGROUND = 6;
    }

    public static class Vert {

        public static final int POSITION = 0;
        public static final int COLOR = 3;
        public static final int TEXCOORD = 4;
        public static final int INSTANCE = 7;
    }

    public static class Stream {

        public static final int _0 = 0;
        public static final int _1 = 1;
    }
}
