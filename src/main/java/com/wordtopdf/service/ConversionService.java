package com.wordtopdf.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

@Service
public class ConversionService {

	private static final Logger logger = LoggerFactory.getLogger(ConversionService.class);

	@Value("${twilio.accountSid}")
	private String accountSid;

	@Value("${twilio.authToken}")
	private String authToken;

	@Value("${twilio.fromWhatsappNumber}")
	private String fromWhatsappNumber;

	public String convertToPdf(String directory) {
		File wordDirectory = new File(directory);
		String pdfFoldername = directory.concat("/pdf/");
		File pdfDirectory = new File(pdfFoldername);

		if (!pdfDirectory.exists() && !pdfDirectory.mkdirs()) {
			throw new IllegalArgumentException("Unable to create PDF directory.");
		}

		List<File> files = Arrays.stream(wordDirectory.listFiles()).filter(
				file -> file.getName().toLowerCase().endsWith(".docx") || file.getName().toLowerCase().endsWith(".doc"))
				.collect(Collectors.toList());

		if (files.isEmpty()) {
			logger.warn("No files found for conversion in directory: {}", directory);
			return "No files found for conversion.";
		}

		for (File file : files) {
			try (FileInputStream fis = new FileInputStream(file);
					ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

				System.out.println("++++++++++++"+file.getName());
				String content = null;
				if (file.getName().toLowerCase().endsWith(".doc")) {
					content = extractContentFromDocFile(fis);
				} else if (file.getName().toLowerCase().endsWith(".docx")) { 
					content = extractContentFromDocxFile(fis);
				} 

				String pdfFileName = file.getName().replaceAll("\\.(doc|docx)$", ".pdf");

				try (FileOutputStream fos = new FileOutputStream(pdfFoldername + pdfFileName)) {
					createPdfFromContent(content, fos);
				}

			} catch (IOException e) {
				logger.error("Failed to convert Word to PDF: {}", e.getMessage());
				return "Failed to convert Word to PDF";
			}
		}

		logger.info("Files converted successfully.");
		return "Files converted successfully.";
	}

	private String extractContentFromDocFile(InputStream inputStream) throws IOException {
		try (HWPFDocument document = new HWPFDocument(inputStream)) {
			Range range = document.getRange();
			return range.text();
		}
	}

	private String extractContentFromDocxFile(InputStream inputStream) throws IOException {
		try (XWPFDocument docxDocument = new XWPFDocument(inputStream)) {
			StringBuilder contentBuilder = new StringBuilder();
			List<XWPFParagraph> paragraphs = docxDocument.getParagraphs();
			for (XWPFParagraph paragraph : paragraphs) {
				contentBuilder.append(paragraph.getText());
				contentBuilder.append("\n");
			}
			return contentBuilder.toString();
		}
	}

	private void createPdfFromContent(String content, OutputStream outputStream) throws IOException {
		PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputStream));
		Document pdfContent = new Document(pdfDocument);
		pdfContent.add(new Paragraph(content));
		pdfContent.close();
	}

	public String sendReport(String pdfPublicUrl, String receiverWhatsappNumber) {
		try {
			// Initialize Twilio
			Twilio.init(accountSid, authToken);
			// Send PDF as WhatsApp message attachment
			Message.creator(new PhoneNumber("whatsapp:" + receiverWhatsappNumber),
					new PhoneNumber("whatsapp:" + fromWhatsappNumber), "Here's your PDF attachment:")
					.setMediaUrl(pdfPublicUrl).create();
			return "PDF sent successfully via WhatsApp.";
		} catch (Exception e) {
			// Log the error or handle it appropriately
			logger.error("Failed to send PDF via WhatsApp: {}", e.getMessage());
			return "Failed to send PDF via WhatsApp: " + e.getMessage();
		}
	}
}