#ifndef SHADER_H
#define SHADER_H
#include <GLES2/gl2.h>
#include <string>
#include <functional>

enum class UNIFORM_TYPE_FV {
    MATRIX4FV,
    VEC3FV
};

static char yuvFragmentShader[] =
        "#version 320 es                                        \n"
        "precision highp float;                                 \n"
        "in vec2 texcoord;                           \n"
        "uniform sampler2D yTexture;                      \n"
        "uniform sampler2D uTexture;                      \n"
        "uniform sampler2D vTexture;                      \n"
        "out vec4 fragColor;                                    \n"
        "\n"
        "void main()                                            \n"
        "{                                                      \n"
        /*"float nx,ny,r,g,b,y,u,v;\n"
        "nx=texcoord.x;\n"
        "ny=texcoord.y;\n"
        "y=texture(yTexture,texcoord).r;\n"
        "u=texture(uTexture,texcoord).r;\n"
        "v=texture(vTexture,texcoord).r;\n"
        "\n"
        "y=1.1643*(y-0.0625);\n"
        "u=u-0.5;\n"
        "v=v-0.5;\n"
        "r=y+1.5958*v;\n"
        "g=y-0.39173*u-0.81290*v;\n"
        "b=y+2.017*u;\n"
        "fragColor = vec4(r,g,b,1.0f);\n"*/
        "// (1) y - 16 (2) rgb * 1.164                          \n"
        "vec3 yuv;                                              \n"
        "yuv.x = texture(yTexture, texcoord).r;                \n"
        "yuv.y = texture(uTexture, texcoord).r - 0.5f;         \n"
        "yuv.z = texture(vTexture, texcoord).r - 0.5f;         \n"
                                                                "\n"
        "mat3 trans = mat3(1, 1 ,1,                             \n"
        "0, -0.34414, 1.772,                                    \n"
        "1.402, -0.71414, 0                                     \n"
        ");                                                     \n"
                                                                "\n"
        "fragColor = vec4(trans*yuv, 1.0f);                  \n"
        "}  \n";

static const char glVertexShader[] =
        "#version 320 es                                        \n"
        "in vec4 position;  \n"
        "in vec4 inputTextureCoordinate;  \n"
        "out highp vec2 texcoord;  \n"
        "\n"
        "void main()  \n"
        "{  \n"
        "  texcoord = inputTextureCoordinate.xy;  \n"
        "  gl_Position = position;  \n"
        "}  \n";

class Shader {
    GLuint shaderProgram;
    bool loaded;
    std::function<void(bool,const char*)> _callback;
    void init(const char *vsdata, const char *frdata);
    GLenum curTexture = GL_TEXTURE0;;
public:
    Shader(const Shader&) = delete;
    Shader(const Shader&&) = delete;
    Shader(const char *vsdata, const char *frdata, std::function<void(bool,const char*)> callback);
    Shader(std::function<void(bool,const char*)> callback): Shader(nullptr, nullptr, callback) {}
    ~Shader();
    bool bind();
    GLuint& program() {return shaderProgram;}
    GLint get_attrib_location(const std::string &name);
    bool set_uniform_fv(const std::string &name, const GLfloat *val, UNIFORM_TYPE_FV type) const;
    bool set_uniform_f(const std::string &name, const GLfloat val) const;
    bool set_uniform_i(const std::string &name, const GLint val) const;
    bool activeTexture(const std::string &name, const GLuint val);
    void dropCurTexture();
};

#endif
