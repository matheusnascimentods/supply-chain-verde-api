-- products repository query: countAllProducts.
SELECT COUNT(*) FROM product WHERE (:supplierId IS NULL OR supplier_id = :supplierId);
