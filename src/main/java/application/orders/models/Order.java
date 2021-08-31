package application.orders.models;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "orders")
/*
 * define O-R mapping of order table
 */
public class Order {

	@Id //primary key
	@Column(name = "orderId", length = 50)
	@GeneratedValue(generator="uuid-generator")
	@GenericGenerator(name="uuid-generator", strategy="uuid")
	String id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	Date date;

	@Basic
	@Column(name = "itemId")
	int itemId;

	@Basic
	@Column(name = "customerId", length = 50)
	String customerId;

	@Basic
	@Column(name = "count")
	int count;

	@Basic
	@Column(name = "notifyMobile")
	String notifyMobile = "false";

	public Order() {
		super();
//		this.id = UUID.randomUUID().toString();
		// TODO Auto-generated constructor stub
	}
	public String getId() {
		return id;
	}
	public void setId(final String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(final int itemId) {
		this.itemId = itemId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(final String customer_id) {
		this.customerId = customer_id;
	}

	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	public String getNotifyMobile() {
		return notifyMobile;
	}

	public void setNotifyMobile(final String notifyMobile) {
		this.notifyMobile = notifyMobile;
	}

	@Override
	public String toString() {
		return "{id = " + id + ", itemId=" + itemId + ", date=" + date.toString() + ", customerId=" + customerId
				+ ", count=" + count + "}";
	}



}
