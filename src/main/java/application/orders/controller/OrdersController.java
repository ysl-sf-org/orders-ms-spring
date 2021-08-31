package application.orders.controller;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import application.orders.config.MFConfig;
import application.orders.models.About;
import application.orders.models.Order;
import application.orders.repository.AboutService;
import application.orders.repository.OrdersRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.boot.json.*;

@RestController
@Api(value="Orders API")
@RequestMapping("/")
public class OrdersController {
	
	private static Logger logger =  LoggerFactory.getLogger(OrdersController.class);
	
	@Autowired
    private MFConfig mfConfig;

    @Autowired
    private OrdersRepository ordersRepo;
    
    @Autowired
    private AboutService aboutService;
    
    /**
	 * @return about orders
	 */
	@ApiOperation(value = "About Orders")
	@GetMapping(path = "/about", produces = "application/json")
	@ResponseBody 
	public About aboutOrders() {
		return aboutService.getInfo();
	}
    
    /**
     * @return get all orders
     */
    @ApiOperation(value = "View the list of orders")
    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    protected @ResponseBody ResponseEntity<?> getOrders() {
        try {
         	final String customerId = getCustomerId();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
        	}
        	
        	logger.debug("caller: " + customerId);
            
        	final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);
        	
            return  ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    private String getCustomerId() {
    	final SecurityContext ctx = SecurityContextHolder.getContext();
    	if (ctx.getAuthentication() == null) {
    		return null;
    	};
    	
    	if (!ctx.getAuthentication().isAuthenticated()) {
    		return null;
    	}
    	
    	final OAuth2Authentication oauth = (OAuth2Authentication)ctx.getAuthentication();
    	
    	logger.debug("CustomerID: " + oauth.getName());
    	
    	return oauth.getName();
    }

    /**
     * @return get orders by id
     */
    @ApiOperation(value = "View the list of orders by order id")
    @RequestMapping(value = "/orders/{id}", method = RequestMethod.GET)
    protected ResponseEntity<?> getById(@RequestHeader Map<String, String> headers, @PathVariable long id) {
		final String customerId = getCustomerId();
		if (customerId == null) {
			// if no user passed in, this is a bad request
			return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
		}
		
		logger.debug("caller: " + customerId);
		
		final Optional<Order> order = ordersRepo.findById(id);
		
		if (order.get().getCustomerId().equals(customerId)) {
			return ResponseEntity.ok(order);
		}
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + id + " not found");
	}

    /**
     * Add order
     * @return transaction status
     */
    @ApiOperation(value = "Place orders")
    @RequestMapping(value = "/orders", method = RequestMethod.POST, consumes = "application/json")
    protected ResponseEntity<?> create(@RequestBody Order payload) {
        try {
        	
        	logger.info("entered post");
			
    		final String customerId = getCustomerId();
			if (customerId == null) {
				// if no user passed in, this is a bad request
				return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
			}
	        
			payload.setDate(Calendar.getInstance().getTime());
			payload.setCustomerId(customerId);
    		logger.info("New order: " + payload.toString());
			
			ordersRepo.save(payload);
			
			final URI location =  ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(payload.getId()).toUri();
			
			notifyShipping(payload);
			
			return ResponseEntity.created(location).build();
        } catch (Exception ex) {
            logger.error("Error creating order: " + ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating order: " + ex.toString());
        }
        
    }
    
   
	/**
	 * Sending order information to the Shipping app
	 * @param order
	 */
    private void notifyShipping(Order order) {
    	
    	// Notify inventory to update the stock
    	
        logger.debug("to do: " + order);

		//Implement shipment service

		//notify customer about order status over mobile
		if ( Boolean.parseBoolean(order.getNotifyMobile()) ) {
			notifyOverMobile(order);
		}
		
	}
	
	private void notifyOverMobile(Order order) {
		//send Mobile Push Notification that Order has been dispatched for Shippment
		String mfTokenEndpointUrl = mfConfig.getAuthUrl();
		String mfPushUrl = mfConfig.getPushUrl();

		if ( mfTokenEndpointUrl != null && !mfTokenEndpointUrl.isEmpty() &&
				mfPushUrl != null && !mfPushUrl.isEmpty()) {
		
			RestTemplate restTemplate = new RestTemplate(); 
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setBasicAuth(mfConfig.getClientId(), mfConfig.getSecret());
			httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			//TODO: use the above variables and get a encoded query params format.  For now hardcoding...
			String httpBody = "grant_type=client_credentials&scope=messages.write%20push.application." + mfConfig.getAppId();

			HttpEntity<String> request  = new HttpEntity(httpBody, httpHeaders);
			ResponseEntity<String> result = restTemplate.exchange(mfTokenEndpointUrl, HttpMethod.POST, request, String.class, new java.util.HashMap());

			JsonParser jsonParser = JsonParserFactory.getJsonParser();
			Map<String,Object> resultJson = jsonParser.parseMap(result.getBody());

			httpHeaders = new HttpHeaders();
			httpHeaders.setBearerAuth((String)resultJson.get("access_token"));
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);

			String notificationMsg = "Your Oder " + order.getId() + " has left the stores for shipment";
			httpBody = "{\"message\": { \"alert\":" + "\"" + notificationMsg + "\"" + "}, \"target\": {\"userIds\":[" + "\"" + order.getCustomerId() + "\"" + "]}}";
			request  = new HttpEntity(httpBody, httpHeaders);
			result = restTemplate.exchange(mfPushUrl, HttpMethod.POST, request, String.class, new java.util.HashMap());
		}
	}

}
