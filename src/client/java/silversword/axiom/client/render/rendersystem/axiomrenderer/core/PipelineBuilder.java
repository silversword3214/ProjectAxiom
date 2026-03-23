package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.mixininterface.ILineSmoothing;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PipelineBuilder {
    private final Object innerBuilder; // RenderPipeline.Builder
    private boolean lineSmooth;

    // Reflektiometodit
    private Method withSnippetMethod;
    private Method withLocationMethod;
    private Method withVertexFormatMethod;
    private Method withVertexShaderMethod;
    private Method withFragmentShaderMethod;
    private Method withDepthTestFunctionMethod;
    private Method withDepthWriteMethod;
    private Method withBlendMethod;
    private Method withCullMethod;
    private Method withUniformMethod;
    private Method withSamplerMethod;
    private Method buildMethod;

    public PipelineBuilder(RenderPipeline.Snippet... snippets) {
        try {
            Class<?> builderClass = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline$Builder");

            Constructor<?> constructor = builderClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            this.innerBuilder = constructor.newInstance();

            withSnippetMethod = builderClass.getDeclaredMethod("withSnippet", RenderPipeline.Snippet.class);
            withLocationMethod = builderClass.getDeclaredMethod("withLocation", Identifier.class);
            withVertexFormatMethod = builderClass.getDeclaredMethod("withVertexFormat", VertexFormat.class, VertexFormat.Mode.class);
            withVertexShaderMethod = builderClass.getDeclaredMethod("withVertexShader", Identifier.class);
            withFragmentShaderMethod = builderClass.getDeclaredMethod("withFragmentShader", Identifier.class);
            withDepthTestFunctionMethod = builderClass.getDeclaredMethod("withDepthTestFunction", DepthTestFunction.class);
            withDepthWriteMethod = builderClass.getDeclaredMethod("withDepthWrite", boolean.class);
            withBlendMethod = builderClass.getDeclaredMethod("withBlend", BlendFunction.class);
            withCullMethod = builderClass.getDeclaredMethod("withCull", boolean.class);
            withUniformMethod = builderClass.getDeclaredMethod("withUniform", String.class, UniformType.class);
            withSamplerMethod = builderClass.getDeclaredMethod("withSampler", String.class);
            buildMethod = builderClass.getDeclaredMethod("build");

            // 4. Asetetaan ne saavutettaviksi (private-metodit)
            withSnippetMethod.setAccessible(true);
            withLocationMethod.setAccessible(true);
            withVertexFormatMethod.setAccessible(true);
            withVertexShaderMethod.setAccessible(true);
            withFragmentShaderMethod.setAccessible(true);
            withDepthTestFunctionMethod.setAccessible(true);
            withDepthWriteMethod.setAccessible(true);
            withBlendMethod.setAccessible(true);
            withCullMethod.setAccessible(true);
            withUniformMethod.setAccessible(true);
            withSamplerMethod.setAccessible(true);
            buildMethod.setAccessible(true);

            // 5. Lisätään snippetit
            for (RenderPipeline.Snippet snippet : snippets) {
                withSnippetMethod.invoke(innerBuilder, snippet);
            }

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Failed to initialize ExtendedRenderPipelineBuilder", e);
        }
    }

    public PipelineBuilder withLocation(Identifier location) {
        try {
            withLocationMethod.invoke(innerBuilder, location);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withVertexFormat(VertexFormat format, VertexFormat.Mode drawMode) {
        try {
            withVertexFormatMethod.invoke(innerBuilder, format, drawMode);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withVertexShader(Identifier shader) {
        try {
            withVertexShaderMethod.invoke(innerBuilder, shader);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withFragmentShader(Identifier shader) {
        try {
            withFragmentShaderMethod.invoke(innerBuilder, shader);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withDepthTestFunction(DepthTestFunction function) {
        try {
            withDepthTestFunctionMethod.invoke(innerBuilder, function);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withDepthWrite(boolean write) {
        try {
            withDepthWriteMethod.invoke(innerBuilder, write);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withBlend(BlendFunction blend) {
        try {
            withBlendMethod.invoke(innerBuilder, blend);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withCull(boolean cull) {
        try {
            withCullMethod.invoke(innerBuilder, cull);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withUniform(String name, UniformType type) {
        try {
            withUniformMethod.invoke(innerBuilder, name, type);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withSampler(String name) {
        try {
            withSamplerMethod.invoke(innerBuilder, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public PipelineBuilder withLineSmooth() {
        this.lineSmooth = true;
        return this;
    }

    public RenderPipeline build() {
        try {
            RenderPipeline pipeline = (RenderPipeline) buildMethod.invoke(innerBuilder);
            ((ILineSmoothing) pipeline).axiom_setLineSmooth(lineSmooth);
            return pipeline;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}