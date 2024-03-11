Word to PDF Conversion and Whatapp Report

URL: /convertToPDF
Description: Converts Word files located in a specified directory to PDF format.

URL: /sendReport
Description: Sends a PDF report via WhatsApp using a public URL.
Sample Request Body:
{
    "pdfFileName":"1234564.pdf",
    "receiverWhatsappNumber": "+919876543210"
}

Configuration
pdf.folderpath: The base folder path where generated PDF files are stored.
word.folderpath: The base folder path where Word files to be converted are located.
twilio.accountSid: Update the AccountSid from Twilio 
twilio.authToken: Update the AuthToken from Twilio
twilio.fromWhatsappNumber: Update the Whatspp Business Number registered in Twilio

Deploy to the server to work as the pdfs should be available to the twilio as a public url to access the PDF
(PDF WILL not be send/stored to Twilio)