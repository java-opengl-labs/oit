/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oit.gl3.ws;

import com.jogamp.opengl.GL3;
import oit.gl3.FullscreenQuad;
import jglm.Jglm;
import jglm.Mat4;
import jglm.Vec2i;
import oit.gl3.Scene;
import oit.gl3.ws.glsl.Final;
import oit.gl3.ws.glsl.Init;

/**
 *
 * @author gbarbieri
 */
public class WeightedSum {

    private int[] accumulationFboId;
    private int[] accumulationTexId;
    private int[] sampler;
    private Vec2i imageSize;
    private Init init;
    private Final finale;
    private FullscreenQuad fullscreenQuad;

    public WeightedSum(GL3 gl3, int blockBinding) {

        initSampler(gl3);

        buildShaders(gl3, blockBinding);
        
        fullscreenQuad = new FullscreenQuad(gl3);
    }

    public void render(GL3 gl3, Scene scene) {
        
        gl3.glDisable(GL3.GL_DEPTH_TEST);
        /**
         * (1) Accumulate (alpha * color) and (alpha).
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, accumulationFboId[0]);
        gl3.glDrawBuffer(GL3.GL_COLOR_ATTACHMENT0);

        gl3.glClearColor(0, 0, 0, 0);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);

        gl3.glBlendEquation(GL3.GL_FUNC_ADD);
        gl3.glBlendFunc(GL3.GL_ONE, GL3.GL_ONE);
        gl3.glEnable(GL3.GL_BLEND);

        init.bind(gl3);
        {
            scene.render(gl3, init.getModelToWorldUL(), init.getAlphaUL());
        }
        init.unbind(gl3);

        gl3.glDisable(GL3.GL_BLEND);
        /**
         * (2) Weighted Sum.
         */
        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
        gl3.glDrawBuffer(GL3.GL_BACK);

        finale.bind(gl3);
        {
            gl3.glUniform3f(finale.getBackgroundColorUL(), 1, 1, 1);

            gl3.glActiveTexture(GL3.GL_TEXTURE0);
            gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);
            gl3.glBindSampler(0, sampler[0]);
            {
                fullscreenQuad.render(gl3);
            }
            gl3.glBindSampler(0, 0);
        }
        finale.unbind(gl3);
    }

    private void buildShaders(GL3 gl3, int blockBinding) {

        String shadersFilepath = "/oit/gl3/ws/glsl/shaders/";

        init = new Init(gl3, shadersFilepath, new String[]{"init_VS.glsl", "shade_VS.glsl"},
                new String[]{"init_FS.glsl", "shade_FS.glsl"}, blockBinding);

        Mat4 modelToClip = Jglm.orthographic2D(0, 1, 0, 1);
        
        finale = new Final(gl3, shadersFilepath, new String[]{"final_VS.glsl"}, new String[]{"final_FS.glsl"});
        finale.bind(gl3);
        {
            gl3.glUniform1i(finale.getColorTexUL(), 0);
            gl3.glUniformMatrix4fv(finale.getModelToClipUL(), 1, false, modelToClip.toFloatArray(), 0);
        }
        finale.unbind(gl3);
    }

    public void reshape(GL3 gl3, int width, int height) {

        imageSize = new Vec2i(width, height);

        deleteRenderTargets(gl3);
        initRenderTargets(gl3);
    }

    private void initRenderTargets(GL3 gl3) {

        accumulationTexId = new int[1];
        gl3.glGenTextures(1, accumulationTexId, 0);

        gl3.glBindTexture(GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0]);

        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_BASE_LEVEL, 0);
        gl3.glTexParameteri(GL3.GL_TEXTURE_RECTANGLE, GL3.GL_TEXTURE_MAX_LEVEL, 0);

        gl3.glTexImage2D(GL3.GL_TEXTURE_RECTANGLE, 0, GL3.GL_RGBA16F,
                imageSize.x, imageSize.y, 0, GL3.GL_RGBA, GL3.GL_FLOAT, null);

        accumulationFboId = new int[1];
        gl3.glGenFramebuffers(1, accumulationFboId, 0);

        gl3.glBindFramebuffer(GL3.GL_FRAMEBUFFER, accumulationFboId[0]);

        gl3.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0,
                GL3.GL_TEXTURE_RECTANGLE, accumulationTexId[0], 0);
    }

    private void deleteRenderTargets(GL3 gl3) {

        if (accumulationFboId != null) {

            gl3.glDeleteFramebuffers(accumulationFboId.length, accumulationFboId, 0);
            accumulationFboId = null;
        }

        if (accumulationTexId != null) {

            gl3.glDeleteFramebuffers(accumulationTexId.length, accumulationTexId, 0);
            accumulationTexId = null;
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
}