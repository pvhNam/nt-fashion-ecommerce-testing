package model;

import java.sql.Date;

public class Voucher {
	private int id;
	private String code;
	private String description;
	private double discountAmount;
	private String discountType;
	private double minOrder;
	private Date expiryDate;
	private Date startDate;
	private boolean publicVoucher = true;

	public Voucher() {
	}

	public Voucher(int id, String code, String description, double discountAmount, String discountType, double minOrder,
			Date expiryDate) {
		this.id = id;
		this.code = code;
		this.description = description;
		this.discountAmount = discountAmount;
		this.discountType = discountType;
		this.minOrder = minOrder;
		this.expiryDate = expiryDate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(double discountAmount) {
		this.discountAmount = discountAmount;
	}

	public String getDiscountType() {
		return discountType;
	}

	public void setDiscountType(String discountType) {
		this.discountType = discountType;
	}

	public double getMinOrder() {
		return minOrder;
	}

	public void setMinOrder(double minOrder) {
		this.minOrder = minOrder;
	}

	public Date getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public boolean isPublicVoucher() {
		return publicVoucher;
	}

	public void setPublicVoucher(boolean publicVoucher) {
		this.publicVoucher = publicVoucher;
	}

	public boolean isStarted() {
		return startDate == null || !startDate.toLocalDate().isAfter(java.time.LocalDate.now());
	}
}