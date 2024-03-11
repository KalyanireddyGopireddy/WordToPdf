package com.wordtopdf.controller;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.wordtopdf.model.SendPdfRequest;
import com.wordtopdf.service.ConversionService;

@RestController
public class ConversionController {

	private static final Logger logger = LoggerFactory.getLogger(ConversionController.class);

	@Value("${pdf.folderpath}")
	private String PDF_FOLDER_PATH;
	
	@Value("${word.folderpath}")
	private String WORD_FOLDER_PATH;

	@Autowired
	private ConversionService conversionService;

	@PostMapping("/convertToPDF")
	public ResponseEntity<String> convertToPdf() {
		try {
			String directory = WORD_FOLDER_PATH;
			logger.info("Received request to convert files in directory: {}", directory);
			String msg = conversionService.convertToPdf(WORD_FOLDER_PATH);
			logger.info("Conversion successful. Response: {}", msg);
			return ResponseEntity.ok(msg);
		} catch (IllegalArgumentException e) {
			logger.error("Bad Request: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			logger.error("Internal Server Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error converting files to PDF.");
		}
	}

	@PostMapping("/sendReport")
	public ResponseEntity<String> sendPdfReport(@RequestBody SendPdfRequest sendPdfRequest, HttpServletRequest request) {
		try {
			String host = request.getServerName();
			int port = request.getServerPort();
			String pdfPublicUrl = "http://" + host + ":" + port + "/pdfPublicUrl/" + sendPdfRequest.getPdfFileName();
			logger.info("PDF public URL: {}", pdfPublicUrl);

			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.getForEntity(pdfPublicUrl, String.class);

			if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
				logger.warn("PDF not found at public URL: {}", pdfPublicUrl);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unable to locate PDF.");
			}

			String sendMsg = conversionService.sendReport(pdfPublicUrl, sendPdfRequest.getReceiverWhatsappNumber());
			logger.info("PDF sent via WhatsApp. Response: {}", sendMsg);
			return ResponseEntity.ok(sendMsg);
		} catch (HttpClientErrorException.NotFound e) {
			logger.error("PDF not found at public URL: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("PDF not found.");
		} catch (Exception e) {
			logger.error("Internal Server Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred: " + e.getMessage());
		}
	}

	@GetMapping("/pdfPublicUrl/{fileName}")
	public ResponseEntity<Resource> generatePdf(@PathVariable String fileName) {
		try {
			Path pdfPath = Paths.get(PDF_FOLDER_PATH, fileName);
			File pdfFile = pdfPath.toFile();

			if (pdfFile.exists()) {
				FileSystemResource resource = new FileSystemResource(pdfFile);
				HttpHeaders headers = new HttpHeaders();
				headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);
				headers.setContentType(MediaType.APPLICATION_PDF);
				return ResponseEntity.ok().headers(headers).contentLength(pdfFile.length())
						.contentType(MediaType.APPLICATION_PDF).body(resource);
			} else {
				logger.warn("PDF file not found: {}", pdfPath);
				return ResponseEntity.notFound().build();
			}
		} catch (Exception e) {
			logger.error("Internal Server Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}