/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.dpo;

import javax.media.opengl.GL3;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec2i;
import oit.gl3.FullscreenQuad;
import oit.gl3.Scene;
import oit.gl3.dpo.glsl.Blend;
import oit.gl3.dpo.glsl.Final;
import oit.gl3.dpo.glsl.Init;
import oit.gl3.dpo.glsl.Opaque;
import oit.gl3.dpo.glsl.Peel;

/**
 *
 * @author gbarbieri
 */
public class DepthPeelingOpaque {

    private Init dpInit;
    private Peel dpPeel;
    private Blend dpBlend;
    private Final dpFinal;
    private Opaque dpOpaque;
    private int[] depthTexId;
    private int[] colorTexId;
    private int[] fboId;
    private int[] colorBlenderTexId;
    private int[] colorBlenderFboId;
    private int[] opaqueFboId;
    private int[] opaqueDepthTexId;
    private int[] opaqueColorTexId;
    private Vec2i imageSize;
    private FullscreenQuad fullscreenQuad;
    private int[] queryId;
    public static int numGeoPasses;
    public int numPasses;
    private boolean useOQ;
    private int[] sampler;

    public DepthPeelingOpaque(GL3 gl3, Vec2i imageSize, int blockBinding) {

        this.imageSize = imageSize;

        buildShaders(gl3, blockBinding);

        initSampler(gl3);

        initQuery(gl3);

        numGeoPasses = 0;
        numPasses = 4;
        useOQ = true;

        fullscreenQuad = new FullscreenQuad(gl3);
    }

    public void render(GL3 gl3, Scene scene) {

        numGeoPasses = 0;
        /**
         * (1) Initialize Opaque Depth Fbo.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, opaqueFboId[0]);
        gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

        gl3.glClearColor(1, 1, 1, 1);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl3.glEnable(GL3.GL_DEPTH_TEST);

        dpOpaque.bind(gl3);
        {
            scene.renderDpOpaque(gl3, dpOpaque.getModelToWorldUL());
        }
        dpOpaque.unbind(gl3);
        /**
         * (2) Initialize Min Depth Buffer.
         */
//        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
//        gl3.glDrawBuffer(GL3.GL_BACK);
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

        gl3.glClearColor(0, 0, 0, 1);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

        gl3.glEnable(GL3.GL_DEPTH_TEST);

        dpInit.bind(gl3);
        {
            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0]);
            gl3.glBindSampler(0, sampler[0]);
            {
                scene.renderDpoTransparent(gl3, dpInit.getModelToWorldUL(), dpInit.getAlphaUL());
            }
            gl3.glBindSampler(0, 0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
        }
        dpInit.unbind(gl3);
        /**
         * (3) Depth Peeling + Blending.
         *
         * careful, && means you wont go deeper of the numLayers, you might be
         * done earlier but for sure not further than that
         */
        int numLayers = (numPasses - 1) * 2;

        for (int layer = 1; useOQ || layer < numLayers; layer++) {
//            System.out.println("layer " + layer);
            int currId = layer % 2;
            int prevId = 1 - currId;

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[currId]);
            gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

            gl3.glClearColor(0, 0, 0, 0);
            gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);

            gl3.glDisable(GL3.GL_BLEND);
            gl3.glEnable(GL3.GL_DEPTH_TEST);

            if (useOQ) {
                gl3.glBeginQuery(GL3.GL_SAMPLES_PASSED, queryId[0]);
            }

            dpPeel.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[prevId]);
                gl3.glBindSampler(0, sampler[0]);
                {
                    gl3.glActiveTexture(GL3.GL_TEXTURE1);
                    gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0]);
                    gl3.glBindSampler(1, sampler[0]);
                    {
                        scene.renderDpoTransparent(gl3, dpPeel.getModelToWorldUL(), dpPeel.getAlphaUL());
                    }
                    gl3.glBindSampler(1, 0);
                }
                gl3.glBindSampler(0, 0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            dpPeel.unbind(gl3);

            if (useOQ) {
                gl3.glEndQuery(GL3.GL_SAMPLES_PASSED);
            }

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
            gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

            gl3.glDisable(GL3.GL_DEPTH_TEST);
            gl3.glEnable(GL3.GL_BLEND);

            gl3.glBlendEquation(GL3.GL_FUNC_ADD);
            gl3.glBlendFuncSeparate(GL3.GL_DST_ALPHA, GL3.GL_ONE, GL3.GL_ZERO, GL3.GL_ONE_MINUS_SRC_ALPHA);

            dpBlend.bind(gl3);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[currId]);
                gl3.glBindSampler(0, sampler[0]);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindSampler(0, 0);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
            }
            dpBlend.unbind(gl3);

            gl3.glDisable(GL3.GL_BLEND);

            if (useOQ) {
                int[] samplesCount = new int[1];
                gl3.glGetQueryObjectuiv(queryId[0], GL3.GL_QUERY_RESULT, samplesCount, 0);
                if (samplesCount[0] == 0) {
                    break;
                }
            }
        }
        /**
         * (4) Final Pass.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL3.GL_BACK);
        gl3.glDisable(GL3.GL_DEPTH_TEST);

        dpFinal.bind(gl3);
        {
            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);
            gl3.glBindSampler(0, sampler[0]);
            {
                gl3.glActiveTexture(GL3.GL_TEXTURE1);
                gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0]);
                gl3.glBindSampler(1, sampler[0]);
                {
                    fullscreenQuad.render(gl3);
                }
                gl3.glBindSampler(1, 0);
            }
            gl3.glBindSampler(0, 0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, 0);
        }
        dpFinal.unbind(gl3);

//        System.out.println("numGeoPasses " + numGeoPasses);
    }

    public void reshape(GL3 gl3, int width, int height) {

        imageSize = new Vec2i(width, height);

        deleteRenderTargets(gl3);
        initRenderTargets(gl3);
    }

    private void buildShaders(GL3 gl3, int blockBinding) {
        System.out.print("buildShaders... ");

        String shadersFilepath = "/oit/gl3/dpo/glsl/shaders/";

        dpInit = new Init(gl3, shadersFilepath, new String[]{"init_VS.glsl", "shade_VS.glsl"},
                new String[]{"init_FS.glsl", "shade_FS.glsl"}, blockBinding);
        dpInit.bind(gl3);
        {
            gl3.glUniform1i(dpInit.getOpaqueDepthTexUL(), 0);
        }
        dpInit.unbind(gl3);

        dpPeel = new Peel(gl3, shadersFilepath, new String[]{"peel_VS.glsl", "shade_VS.glsl"},
                new String[]{"peel_FS.glsl", "shade_FS.glsl"}, blockBinding);
        dpPeel.bind(gl3);
        {
            gl3.glUniform1i(dpPeel.getDepthTexUL(), 0);
            gl3.glUniform1i(dpPeel.getOpaqueDepthTexUL(), 1);
        }
        dpPeel.unbind(gl3);

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);

        dpBlend = new Blend(gl3, shadersFilepath, "blend_VS.glsl", "blend_FS.glsl");
        dpBlend.bind(gl3);
        {
            gl3.glUniform1i(dpBlend.getTempTexUL(), 0);
            gl3.glUniformMatrix4fv(dpBlend.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        dpBlend.unbind(gl3);

        dpFinal = new Final(gl3, shadersFilepath, "final_VS.glsl", "final_FS.glsl");
        dpFinal.bind(gl3);
        {
            gl3.glUniform1i(dpFinal.getColorTexUL(), 0);
            gl3.glUniform1i(dpFinal.getOpaqueColorTexUL(), 1);
            gl3.glUniformMatrix4fv(dpFinal.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        dpFinal.unbind(gl3);

        dpOpaque = new Opaque(gl3, shadersFilepath, "opaque_VS.glsl", "opaque_FS.glsl", blockBinding);

        System.out.println("ok");
    }

    private void initRenderTargets(GL3 gl3) {
        /**
         * Default Depth Peeling resources.
         */
        depthTexId = new int[2];
        colorTexId = new int[2];
        fboId = new int[2];

        gl3.glGenTextures(depthTexId.length, depthTexId, 0);
        gl3.glGenTextures(colorTexId.length, colorTexId, 0);
        gl3.glGenFramebuffers(fboId.length, fboId, 0);

        for (int i = 0; i < 2; i++) {

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, depthTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_DEPTH_COMPONENT32F,
                    imageSize.x, imageSize.y, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);

            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorTexId[i]);

            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

            gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                    imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

            gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[i]);

            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                    GL3.GL_TEXTURE_RECTANGLE, depthTexId[i], 0);
            gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                    GL3.GL_TEXTURE_RECTANGLE, colorTexId[i], 0);
        }
        colorBlenderTexId = new int[1];
        gl3.glGenTextures(1, colorBlenderTexId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        colorBlenderFboId = new int[1];
        gl3.glGenFramebuffers(1, colorBlenderFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, colorBlenderFboId[0]);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                GL3.GL_TEXTURE_RECTANGLE, depthTexId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, colorBlenderTexId[0], 0);
        /**
         * Opaque resources.
         */
        opaqueDepthTexId = new int[1];
        opaqueColorTexId = new int[1];
        opaqueFboId = new int[1];
        gl3.glGenTextures(opaqueDepthTexId.length, opaqueDepthTexId, 0);
        gl3.glGenTextures(opaqueColorTexId.length, opaqueColorTexId, 0);
        gl3.glGenFramebuffers(opaqueFboId.length, opaqueFboId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_DEPTH_COMPONENT32F,
                imageSize.x, imageSize.y, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT, null);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA,
                imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, opaqueFboId[0]);

        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT,
                GL3.GL_TEXTURE_RECTANGLE, opaqueDepthTexId[0], 0);
        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, opaqueColorTexId[0], 0);
    }

    private void deleteRenderTargets(GL3 gl3) {

        if (fboId != null) {
            gl3.glDeleteFramebuffers(fboId.length, fboId, 0);
            fboId = null;
        }
        if (colorBlenderFboId != null) {
            gl3.glDeleteFramebuffers(colorBlenderFboId.length, colorBlenderFboId, 0);
            colorBlenderFboId = null;
        }
        if (depthTexId != null) {
            gl3.glDeleteTextures(depthTexId.length, depthTexId, 0);
            depthTexId = null;
        }
        if (colorTexId != null) {
            gl3.glDeleteTextures(colorTexId.length, colorTexId, 0);
            colorTexId = null;
        }
        if (colorBlenderTexId != null) {
            gl3.glDeleteTextures(colorBlenderTexId.length, colorBlenderTexId, 0);
            colorBlenderTexId = null;
        }
        if (opaqueFboId != null) {
            gl3.glDeleteFramebuffers(opaqueFboId.length, opaqueFboId, 0);
            opaqueFboId = null;
        }
        if (opaqueColorTexId != null) {
            gl3.glDeleteFramebuffers(opaqueColorTexId.length, opaqueColorTexId, 0);
            opaqueColorTexId = null;
        }
        if (opaqueDepthTexId != null) {
            gl3.glDeleteTextures(opaqueDepthTexId.length, opaqueDepthTexId, 0);
            opaqueDepthTexId = null;
        }
    }

    private void initSampler(GL3 gl3) {

        sampler = new int[1];
        gl3.glGenSamplers(1, sampler, 0);

        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
        gl3.glSamplerParameteri(sampler[0], GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
    }

    private void initQuery(GL3 gl3) {

        queryId = new int[1];
        gl3.glGenQueries(1, queryId, 0);
    }
}
