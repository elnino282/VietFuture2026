-- Add batching fields to delivery_orders
ALTER TABLE delivery_orders 
ADD COLUMN requested_delivery_date DATE,
ADD COLUMN delivery_zone_to VARCHAR(50);

