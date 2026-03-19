package silversword.axiom.client.utils.render;

import silversword.axiom.ProjectAxiom;
import silversword.axiom.client.render.font.FontFace;
import silversword.axiom.client.render.font.FontInfo;
import silversword.axiom.client.render.font.FontFamily;
import silversword.axiom.client.render.font.BuiltinFontFace;
import silversword.axiom.client.render.font.SystemFontFace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

public class FontUtils {
    private FontUtils() {}

    public static FontInfo getSysFontInfo(File file) {
        return getFontInfo(stream(file));
    }

    public static FontInfo getBuiltinFontInfo(String builtin) {
        return getFontInfo(stream(builtin));
    }

    public static FontInfo getFontInfo(InputStream stream) {
        if (stream == null) return null;

        byte[] bytes = readBytes(stream);
        if (bytes.length < 5) return null;

        if (
            bytes[0] != 0 ||
            bytes[1] != 1 ||
            bytes[2] != 0 ||
            bytes[3] != 0 ||
            bytes[4] != 0
        ) return null;

        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, buffer)) return null;

        ByteBuffer nameBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 1);
        ByteBuffer typeBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 2);
        if (typeBuffer == null || nameBuffer == null) return null;

        return new FontInfo(
            StandardCharsets.UTF_16.decode(nameBuffer).toString(),
            FontInfo.Type.fromString(StandardCharsets.UTF_16.decode(typeBuffer).toString())
        );
    }

    public static Set<String> getSearchPaths() {
        Set<String> paths = new HashSet<>();
        paths.add(System.getProperty("java.home") + "/lib/fonts");

        for (File dir : getUFontDirs()) {
            if (dir.exists()) paths.add(dir.getAbsolutePath());
        }

        for (File dir : getSFontDirs()) {
            if (dir.exists()) paths.add(dir.getAbsolutePath());
        }

        return paths;
    }

    public static List<File> getUFontDirs() {
        return switch (net.minecraft.util.Util.getOperatingSystem()) {
            case WINDOWS -> List.of(new File(System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Windows\\Fonts"));
            case OSX -> List.of(new File(System.getProperty("user.home") + "/Library/Fonts/"));
            default -> List.of(new File(System.getProperty("user.home") + "/.local/share/fonts"), new File(System.getProperty("user.home") + "/.fonts"));
        };
    }

    public static List<File> getSFontDirs() {
        return switch (net.minecraft.util.Util.getOperatingSystem()) {
            case WINDOWS -> List.of(new File("C:\\Windows\\Fonts"));
            case OSX -> List.of(new File("/Library/Fonts"), new File("/System/Library/Fonts"));
            default -> List.of(new File("/usr/share/fonts"), new File("/usr/local/share/fonts"));
        };
    }

    public static void loadBuiltin(List<FontFamily> fontList, String builtin) {
        FontInfo info = getBuiltinFontInfo(builtin);
        if (info != null) {
            FontFamily fam = getFamily(fontList, info.family());
            if (fam == null) {
                fam = new FontFamily(info.family());
                fontList.add(fam);
            }
            fam.addFont(new BuiltinFontFace(info, builtin));
        }
    }

    public static void loadSystem(List<FontFamily> fontList, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadSystem(fontList, file);
            } else if (file.getName().toLowerCase().endsWith(".ttf") || file.getName().toLowerCase().endsWith(".otf")) {
                FontInfo info = getSysFontInfo(file);
                if (info == null) continue;
                FontFamily fam = getFamily(fontList, info.family());
                if (fam == null) {
                    fam = new FontFamily(info.family());
                    fontList.add(fam);
                }
                fam.addFont(new SystemFontFace(info, file.toPath()));
            }
        }
    }

    private static FontFamily getFamily(List<FontFamily> list, String name) {
        for (FontFamily f : list) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    public static boolean addFont(List<FontFamily> fontList, FontFace font) {
        FontFamily family = getFamily(fontList, font.info.family());
        if (family == null) {
            family = new FontFamily(font.info.family());
            fontList.add(family);
        }
        if (family.hasType(font.info.type())) return false;
        return family.addFont(font);
    }

    public static InputStream stream(String builtin) {
        return FontUtils.class.getResourceAsStream("/assets/" + ProjectAxiom.MOD_ID + "/fonts/" + builtin + ".ttf");
    }

    private static byte[] readBytes(InputStream in) {
        if (in == null) return new byte[0];
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static InputStream stream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}