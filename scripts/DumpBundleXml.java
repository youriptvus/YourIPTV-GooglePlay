import com.android.aapt.Resources;
import java.io.InputStream;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Read an AAB's compiled resource XML using bundletool's official AAPT protobuf classes. */
public final class DumpBundleXml {
    private static Element element(Document document, Resources.XmlElement source) {
        String namespace = source.getNamespaceUri().isEmpty() ? null : source.getNamespaceUri();
        Element result = document.createElementNS(namespace, source.getName());
        for (Resources.XmlAttribute attribute : source.getAttributeList()) {
            String uri = attribute.getNamespaceUri().isEmpty() ? null : attribute.getNamespaceUri();
            result.setAttributeNS(uri, attribute.getName(), attribute.getValue());
        }
        for (Resources.XmlNode child : source.getChildList()) {
            if (child.hasElement()) result.appendChild(element(document, child.getElement()));
            else if (!child.getText().isEmpty()) result.appendChild(document.createTextNode(child.getText()));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[1].matches("base/res/xml(?:-[A-Za-z0-9_-]+)?/[A-Za-z0-9_.-]+\\.xml")) {
            throw new IllegalArgumentException("Expected AAB path and base/res/xml[-qualifier]/ resource entry");
        }
        try (ZipFile archive = new ZipFile(args[0])) {
            var entry = archive.getEntry(args[1]);
            if (entry == null || entry.isDirectory() || entry.getSize() > 2 * 1024 * 1024) {
                throw new IllegalArgumentException("Missing or oversized compiled XML resource");
            }
            Resources.XmlNode root;
            try (InputStream input = archive.getInputStream(entry)) {
                root = Resources.XmlNode.parseFrom(input);
            }
            if (!root.hasElement()) throw new IllegalArgumentException("Compiled XML has no root element");
            var builder = DocumentBuilderFactory.newInstance();
            builder.setNamespaceAware(true);
            Document document = builder.newDocumentBuilder().newDocument();
            document.appendChild(element(document, root.getElement()));
            var factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(System.out));
        }
    }
}
