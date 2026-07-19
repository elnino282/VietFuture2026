-- Add pre-order fields to marketplace_products
ALTER TABLE marketplace_products 
ADD COLUMN allows_pre_order BOOLEAN DEFAULT FALSE,
ADD COLUMN earliest_fulfillment_date DATE;

-- Add pre-order fields to marketplace_orders
ALTER TABLE marketplace_orders
ADD COLUMN is_pre_order BOOLEAN DEFAULT FALSE,
ADD COLUMN requested_delivery_date DATE,
ADD COLUMN harvest_ready_date DATE;

