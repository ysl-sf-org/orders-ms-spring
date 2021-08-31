package application.orders.repository;

import org.springframework.stereotype.Service;

import application.orders.models.About;

@Service
public class AboutServiceImpl implements AboutService {

	@Override
	public About getInfo() {
		// TODO Auto-generated method stub
		return new About("Orders Service", "Storefront", "Stores all the order information");
	}

}
