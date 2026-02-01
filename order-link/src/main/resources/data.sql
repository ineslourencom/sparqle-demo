INSERT INTO order_request_entries (
	merchant_id,
	merchant_ref,
	barcode,
	recipient_name,
	recipient_address1,
	recipient_address2,
	recipient_postal_code,
	recipient_city,
	recipient_country,
	parcel_weight_grams,
	parcel_length_cm,
	parcel_width_cm,
	parcel_height_cm
) VALUES
	(101, 'REF-ABC-123', 'TRACKABCDE1', 'Alice Doe', '12 Rue Lafayette', NULL, '75009', 'Paris', 'FR', 1250, 32.5, 22.0, 12.5),
	(202, 'REF-XYZ-789', 'TRACKXYZ7890', 'Bob Smith', '742 Evergreen Ter', 'Apt 3', '60601', 'Chicago', 'US', 980, 28.0, 18.0, 14.0);

INSERT INTO inventory_entries (order_request_id, status, tracking_ref, details)
SELECT order_request_id, 'INVENTORY_RESERVED', 'INV-REF-001', 'Inventory reserved for shipment'
FROM order_request_entries
WHERE merchant_ref = 'REF-ABC-123';

INSERT INTO inventory_entries (order_request_id, status, tracking_ref, details)
SELECT order_request_id, 'COMPLETED', 'INV-REF-002', 'Order delivered successfully'
FROM order_request_entries
WHERE merchant_ref = 'REF-XYZ-789';