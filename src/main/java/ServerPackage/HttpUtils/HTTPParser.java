/**
 * Author name : Shubham Pareek
 * Author email : spareek@dons.usfca.edu
 * Class purpose : parsing the http request
 */
package ServerPackage.HttpUtils;

import ServerPackage.HttpUtils.Validators.HttpRequestValidator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;

public class HTTPParser {

    
    private static final Logger LOGGER = LogManager.getLogger(HTTPParser.class);

    private InputStream inputStream;
    private String fullRequest;
    private HashMap <String, String> header;
    private StringBuffer messageBody;
    private String body;
    private HashMap<String, String> request;
    private boolean requestIsValid;
    private boolean doneProcessingRequest;

    public HTTPParser (InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        this.header = new HashMap<>();
        this.messageBody = new StringBuffer();
        this.request = new HashMap<>();
        this.requestIsValid = true;
        this.doneProcessingRequest = false;
        fullRequest = generateFullRequest(this.inputStream);
        LOGGER.info("Full request is : " + fullRequest);
        LOGGER.info("Length of full request is " + fullRequest.length());
        parseFullRequest(fullRequest);
    }

    //did not make this method static on purpose, since we might have different threads accessing it, and might want to add more functionality
    private String generateFullRequest (InputStream inputStream) throws IOException {
        //method takes in an input stream, then reads all the input and returns the full string of the http request
        //https://medium.com/@himalee.tailor/reading-a-http-request-29edd181a6c5 to learn how to parse the entire http request
        StringBuilder request = new StringBuilder();
        do {
            request.append((char) inputStream.read());
        } while (inputStream.available() > 0);
        String parsedFullRequest =  request.toString();
        return parsedFullRequest;
    }

    private void parseFullRequest(String fullRequest) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(fullRequest));

        //the first line will always be the request, so we pass the first line to the generateRequest method
        /**Getting the full request**/
        request = generateRequest(reader.readLine());
        LOGGER.info("Received request : " + request + " valid request : " + requestIsValid);
        //if request is not valid, we return
        if (!requestIsValid){
            LOGGER.info("Got incorrect request");
            return;
        }

        //next to process the headers, we keep sending it in line by line till we encounter an empty line
        String headerLine = reader.readLine();
        //this check is there, in case we get a request with no headers
        if (headerLine != null) {
            while (headerLine.length() > 0) {
                //generating header
                header = generateHeader(headerLine, header);
                if (!requestIsValid) break;
                headerLine = reader.readLine();
            }
        } else {
            requestIsValid = false;
        }

        //if request is not valid, we return
        if (!requestIsValid){
            LOGGER.info("Got incorrect header format");
            return;
        }

        requestIsValid = HttpRequestValidator.validateAllHeader(header);

        if (!requestIsValid){
            LOGGER.info("Invalid headers, no Host field");
            return;
        }

        LOGGER.info("Parsed headers, total number is : " + header.size());


        //now we get the body
        String bodyLine = reader.readLine();
        //we do the same as we did for headers, but this time with  a string instead of a hashmap
        while (bodyLine != null){
            messageBody = generateBody(bodyLine, messageBody);
            bodyLine = reader.readLine();
        }

        body = messageBody.toString();
        String requestType = getRequestType();
        requestIsValid = HttpRequestValidator.validateBody(header, requestType, body);

        LOGGER.info("Body is : " + getBody());

        return;
    }

    private HashMap<String, String> generateRequest (String requestToBeParsed) {
        String generatedRequest = requestToBeParsed;

        LOGGER.info ("Full request received is : \n" + generatedRequest);

        requestIsValid = HttpRequestValidator.validateRequest(generatedRequest);

        if (requestIsValid){
            HashMap<String, String> parsedRequest = HttpRequestGenerator.generateRequest(generatedRequest);
            return parsedRequest;
        }

        return new HashMap<>();
    }

    private HashMap<String, String> generateHeader (String headerLine, HashMap<String, String> header) {
//        LOGGER.info("Parsing header : " + headerLine);

        requestIsValid = HttpRequestValidator.validateHeader(headerLine);

//        LOGGER.info("Parsing header is valid : " + requestIsValid);

        if (requestIsValid){
            HttpRequestGenerator.generateHeader(headerLine, header);
            return header;
        }

        return new HashMap<>();
    }

    private StringBuffer generateBody (String bodyLine, StringBuffer messageBody) {
        if (requestIsValid){
            messageBody = HttpRequestGenerator.generateBody(bodyLine, messageBody);
            return messageBody;
        }
        return new StringBuffer();
    }

    public String cleanBody (String body){
        /**
         * This is mainly for the test cases, as the http fetcher is sending an already clean body
         */
        if (!body.startsWith("message=")){
            return body.strip();
        }
        LOGGER.info("Received body " + body);
        String[] splitBody = body.split("=",2);
        String uncleanBody = splitBody[1].strip();
        String[] brokenDownBody = uncleanBody.split("\\+");
        String cleanBody = String.join(" ", brokenDownBody).strip();

        LOGGER.info("Body after cleaning is : " + cleanBody);
        return cleanBody;
    }

    public HashMap<String, String> getHeader() {
        return header;
    }

    public String getBody() {
        //send unfiltered body
        return body.strip();
    }

    public String getRequestType () {
        return request.get("Type");
    }

    public String getRequestPath () {
        return request.get("Path");
    }

    public String getRequestHttpVersion () {
        return request.get("HttpVersion");
    }

    public HashMap<String, String> getRequest () {
        return request;
    }

    public boolean isRequestIsValid (){
        return requestIsValid;
    }
}
