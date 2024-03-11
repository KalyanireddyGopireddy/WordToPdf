package com.wordtopdf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPdfRequest {

	private String pdfFileName;
	private String receiverWhatsappNumber;
}
