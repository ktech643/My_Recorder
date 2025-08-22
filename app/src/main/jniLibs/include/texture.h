#ifndef TEXTURE_H
#define TEXTURE_H

#include <GLES3/gl3.h>

#include <cstdint>
#include <string>

class Texture {
public:
    enum class INPUT_TYPE {
        RGBA,
        BGRA,
        YUV420
    };
private:
    int _width, _height;
    INPUT_TYPE _type;
    GLuint _tex_id[1];
    bool _loaded;
    std::string _error;
    void convert_bgra_rgba(uint8_t *data, int width, int height);
public:
    static std::string checkGlError(const std::string &after);
    bool initTexture();
    Texture();
    ~Texture();
    bool loaded() { return _loaded;}
    /**
     * Update texture
     * @param data
     * @param width
     * @param height
     * @param type
     * @return true if textureid changed
     */
    bool add_buffer(uint8_t *data, int width, int height, INPUT_TYPE type);
    GLuint texture_id();
    bool has_error();
    const std::string& error() const;
};

#endif // TEXTURE_H