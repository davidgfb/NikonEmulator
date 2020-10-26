package com.nikonhacker;

//<editor-fold defaultstate="collapsed" desc="imports">
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import static java.lang.System.out;
import java.nio.charset.Charset;
//</editor-fold>

public class XStreamUtils {
    
    //<editor-fold defaultstate="collapsed" desc="getters">
    public static XStream getBaseXStream() {
        return new XStream(new StaxDriver()) {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        //noinspection SimplifiableIfStatement
                        if (definedIn == Object.class) {
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };
    }
//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="metodos">
    public static void save(Object object, OutputStream outputStream) {
        save(object, outputStream, getBaseXStream());
    }

    public static void save(Object object, OutputStream outputStream, XStream xStream) {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
            xStream.toXML(object, writer);
        }
        finally {
            try {
                if (writer!= null) writer.close();
                if (outputStream!=null) outputStream.close();
            } catch (IOException e) {
                out.println(e);
                //noop
            }
        }
    }

//</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="funciones">
    public static Object load(InputStream inputStream) {
        return load(inputStream, getBaseXStream());
    }

    public static Object load(InputStream inputStream, XStream xStream) {
        Reader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        return xStream.fromXML(reader);
    }

//</editor-fold>
}
