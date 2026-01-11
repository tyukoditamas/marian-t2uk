package org.app.xml;

import org.app.model.ExcelData;
import org.app.model.Item;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class XmlGenerator {
    private static final String TEMPLATE_RESOURCE = "test/t2uk UPS 280 art.xml";
    private static final DateTimeFormatter OUTPUT_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    public File buildOutputFile(File outputDirectory) {
        return new File(outputDirectory, "T2-" + LocalDateTime.now().format(OUTPUT_STAMP) + ".xml");
    }

    public void generate(File outputFile, String lrn, ExcelData data) throws Exception {
        Document template = loadTemplate();
        applyData(template, lrn, data);
        writeXml(template, outputFile);
    }

    private Document loadTemplate() throws Exception {
        try (InputStream inputStream = XmlGenerator.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template XML not found in resources: " + TEMPLATE_RESOURCE);
            }
            String xml = readToString(inputStream);
            int start = xml.indexOf("<?xml");
            if (start > 0) {
                xml = xml.substring(start);
            }
            if (!xml.isEmpty() && xml.charAt(0) == '\uFEFF') {
                xml = xml.substring(1);
            }
            SAXBuilder builder = new SAXBuilder();
            return builder.build(new StringReader(xml));
        }
    }

    private void applyData(Document document, String lrn, ExcelData data) {
        Element root = document.getRootElement();

        Element transitOperation = root.getChild("TransitOperation");
        if (transitOperation != null) {
            Element lrnElement = transitOperation.getChild("LRN");
            if (lrnElement != null) {
                lrnElement.setText(lrn);
            }
            Element limitDate = transitOperation.getChild("limitDate");
            if (limitDate != null) {
                limitDate.setText(LocalDate.now().toString());
            }
        }

        Element consignment = root.getChild("Consignment");
        if (consignment == null) {
            throw new IllegalStateException("Template is missing Consignment.");
        }
        Element consignmentGross = consignment.getChild("grossMass");
        if (consignmentGross != null) {
            consignmentGross.setText(formatDecimal(data.getTotalGrossMass()));
        }

        Element houseConsignment = consignment.getChild("HouseConsignment");
        if (houseConsignment == null) {
            throw new IllegalStateException("Template is missing HouseConsignment.");
        }
        Element houseGross = houseConsignment.getChild("grossMass");
        if (houseGross != null) {
            houseGross.setText(formatDecimal(data.getTotalGrossMass()));
        }

        List<Element> existingItems = houseConsignment.getChildren("ConsignmentItem");
        if (existingItems.isEmpty()) {
            throw new IllegalStateException("Template has no ConsignmentItem entries.");
        }
        Element itemTemplate = existingItems.get(0).clone();
        houseConsignment.removeChildren("ConsignmentItem");

        int index = 1;
        for (Item item : data.getItems()) {
            Element consignmentItem = itemTemplate.clone();
            Element goodsItemNumber = consignmentItem.getChild("goodsItemNumber");
            if (goodsItemNumber != null) {
                goodsItemNumber.setText(String.valueOf(index));
            }
            Element declarationGoodsItemNumber = consignmentItem.getChild("declarationGoodsItemNumber");
            if (declarationGoodsItemNumber != null) {
                declarationGoodsItemNumber.setText(String.valueOf(index));
            }

            Element commodity = consignmentItem.getChild("Commodity");
            if (commodity != null) {
                Element description = commodity.getChild("descriptionOfGoods");
                if (description != null) {
                    description.setText(item.getDescription());
                }
                Element commodityCode = commodity.getChild("CommodityCode");
                if (commodityCode != null) {
                    Element harmonized = commodityCode.getChild("harmonizedSystemSubHeadingCode");
                    if (harmonized != null) {
                        harmonized.setText(item.getCode());
                    }
                }
                Element goodsMeasure = commodity.getChild("GoodsMeasure");
                if (goodsMeasure != null) {
                    Element grossMass = goodsMeasure.getChild("grossMass");
                    if (grossMass != null) {
                        grossMass.setText(formatDecimal(item.getKgs()));
                    }
                }
            }

            Element previousDocument = consignmentItem.getChild("PreviousDocument");
            if (previousDocument != null) {
                Element referenceNumber = previousDocument.getChild("referenceNumber");
                if (referenceNumber != null) {
                    referenceNumber.setText(item.getAwb());
                }
            }

            houseConsignment.addContent(consignmentItem);
            index++;
        }
    }

    private void writeXml(Document document, File outputFile) throws IOException {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile),
                StandardCharsets.UTF_8)) {
            outputter.output(document, writer);
        }
    }

    private String formatDecimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    private String readToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
