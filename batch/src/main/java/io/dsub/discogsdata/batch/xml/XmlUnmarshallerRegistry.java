package io.dsub.discogsdata.batch.xml;

import io.dsub.discogsdata.batch.dump.enums.DumpType;
import io.dsub.discogsdata.batch.xml.object.XmlArtist;
import io.dsub.discogsdata.batch.xml.object.XmlLabel;
import io.dsub.discogsdata.batch.xml.object.XmlMaster;
import io.dsub.discogsdata.batch.xml.object.XmlRelease;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class XmlUnmarshallerRegistry {

    private static final Map<DumpType, Unmarshaller> DUMP_OBJECT_MAP = new HashMap<>();

    public XmlUnmarshallerRegistry() {
        register(DumpType.ARTIST, make(XmlArtist.class));
        register(DumpType.LABEL, make(XmlLabel.class));
        register(DumpType.MASTER, make(XmlMaster.class));
        register(DumpType.RELEASE, make(XmlRelease.class));
    }

    private Unmarshaller make(Class<?> classToBeBound) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(classToBeBound);
        return marshaller;
    }

    public void register(DumpType type, Unmarshaller unmarshaller) {
        DUMP_OBJECT_MAP.put(type, unmarshaller);
    }

    public Unmarshaller resolve(DumpType type) {
        return DUMP_OBJECT_MAP.get(type);
    }
}
