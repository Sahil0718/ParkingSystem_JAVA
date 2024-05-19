CREATE TABLE IF NOT EXISTS parked_cars (
    id INT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    parking_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    removal_timestamp TIMESTAMP NULL
);
select * from parked_cars;
drop table parked_cars;