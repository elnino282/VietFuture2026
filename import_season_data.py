import mysql.connector
from datetime import date
import logging

# Cấu hình logging để dễ theo dõi
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Thông tin cấu hình DB từ file .env
DB_CONFIG = {
    'host': 'localhost',
    'port': 3307,
    'user': 'springuser',
    'password': 'springpass',
    'database': 'season_db'
}

def reset_database(cursor):
    """
    Quét toàn bộ các bảng, bỏ qua Flyway và tiến hành TRUNCATE để reset Data và ID
    """
    logging.info("Bắt đầu tiến trình Reset Database...")
    
    # 1. Tắt Foreign Key Checks
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")
    
    # 2. Lấy danh sách các bảng
    cursor.execute("SHOW TABLES;")
    tables = cursor.fetchall()
    
    # 3. Truncate từng bảng, bỏ qua flyway
    for (table_name,) in tables:
        if table_name.lower() == 'flyway_schema_history':
            logging.info("Bỏ qua bảng cấu trúc của Flyway: flyway_schema_history")
            continue
            
        logging.info(f"Đang làm sạch (TRUNCATE) bảng: {table_name}")
        cursor.execute(f"TRUNCATE TABLE `{table_name}`;")
        
    # 4. Bật lại Foreign Key Checks
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")
    logging.info("Reset Database hoàn tất. ID đã được đưa về 1.")


def import_season_data(cursor):
    """
    Import dữ liệu Mùa vụ với tất cả các cột thực tế
    """
    logging.info("Bắt đầu Import dữ liệu bảng 'seasons'...")
    
    # Câu lệnh INSERT. Lưu ý: Không truyền season_id, để AUTO_INCREMENT tự xử lý (sẽ bắt đầu từ 1)
    insert_query = """
        INSERT INTO seasons (
            season_name, plot_id, crop_id, variety_id, 
            start_date, planned_harvest_date, end_date, 
            status, initial_plant_count, current_plant_count, 
            expected_yield_kg, actual_yield_kg, budget_amount, notes
        ) VALUES (
            %s, %s, %s, %s, 
            %s, %s, %s, 
            %s, %s, %s, 
            %s, %s, %s, %s
        )
    """
    
    # Bộ dữ liệu thực tế: 
    seasons_data = [
        (
            "Mùa Vụ Đông Xuân - Lúa Đài Thơm 2026", # season_name
            1,                                      # plot_id (Lô đất số 1)
            1,                                      # crop_id (Loại cây: Lúa)
            1,                                      # variety_id (Giống lúa Đài Thơm)
            date(2026, 1, 15),                      # start_date
            date(2026, 5, 10),                      # planned_harvest_date
            None,                                   # end_date (Chưa kết thúc)
            "IN_PROGRESS",                          # status
            100000,                                 # initial_plant_count (100k khóm)
            98500,                                  # current_plant_count (hao hụt nhẹ)
            15000.50,                               # expected_yield_kg (15 Tấn)
            None,                                   # actual_yield_kg (Chưa thu hoạch)
            50000000.00,                            # budget_amount (50 Triệu VNĐ)
            "Khí hậu thuận lợi, cần theo dõi sâu cuốn lá" # notes
        ),
        (
            "Mùa Vụ Hè Thu - Xoài Cát Hòa Lộc 2025", 
            2,                                      
            2,                                      
            3,                                      
            date(2025, 6, 1),                       
            date(2025, 12, 15),                     
            date(2025, 12, 20),                     # Đã kết thúc
            "COMPLETED",                            
            500,                                    # 500 cây xoài
            480,                                    
            8000.00,                                # Dự kiến 8 Tấn
            8250.75,                                # Thực tế đạt 8.2 Tấn (Vượt kế hoạch)
            30000000.00,                            
            "Đã thu hoạch và phân phối xong cho siêu thị, chất lượng rất tốt." 
        )
    ]
    
    # Thực thi hàng loạt (Bulk Insert)
    cursor.executemany(insert_query, seasons_data)
    logging.info(f"Đã import thành công {cursor.rowcount} record vào bảng 'seasons'.")


def import_task_data(cursor):
    """
    Import dữ liệu Công việc liên kết với Mùa vụ
    """
    logging.info("Bắt đầu Import dữ liệu bảng 'tasks'...")
    
    insert_query = """
        INSERT INTO tasks (
            user_id, season_id, title, description,
            planned_date, due_date, status, 
            actual_start_date, actual_end_date, notes
        ) VALUES (
            %s, %s, %s, %s,
            %s, %s, %s,
            %s, %s, %s
        )
    """
    
    # Lưu ý: Vì ta đã reset ID và chèn 2 vụ, season_id sẽ chắc chắn là 1 và 2.
    tasks_data = [
        (
            1,                           # user_id (Người quản lý/Nông dân)
            1,                           # season_id (Liên kết Vụ Đông Xuân ID=1)
            "Phun thuốc trừ sâu cuốn lá",# title
            "Phun thuốc đặc trị phòng ngừa sâu cuốn lá giai đoạn làm đòng", # desc
            date(2026, 3, 10),           # planned_date
            date(2026, 3, 12),           # due_date
            "PLANNED",                   # status
            None,                        # actual_start_date
            None,                        # actual_end_date
            "Nhớ mang đủ đồ bảo hộ"      # notes
        ),
        (
            2,                           # user_id
            2,                           # season_id (Liên kết Vụ Hè Thu ID=2)
            "Thu hoạch Xoài",            # title
            "Bao bọc và thu hái cẩn thận",# desc
            date(2025, 12, 10),          # planned_date
            date(2025, 12, 15),          # due_date
            "COMPLETED",                 # status
            date(2025, 12, 11),          # actual_start_date
            date(2025, 12, 15),          # actual_end_date
            "Năng suất vượt mong đợi"    # notes
        )
    ]
    
    cursor.executemany(insert_query, tasks_data)
    logging.info(f"Đã import thành công {cursor.rowcount} record vào bảng 'tasks'.")


def main():
    try:
        # Khởi tạo kết nối
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        
        # 1. Reset Database toàn diện
        reset_database(cursor)
        
        # 2. Chạy Import dữ liệu
        import_season_data(cursor)
        import_task_data(cursor)
        
        # 3. Lưu thay đổi
        conn.commit()
        logging.info("✅ HOÀN TẤT: Toàn bộ quá trình Reset và Import diễn ra thành công!")
        
    except mysql.connector.Error as err:
        logging.error(f"❌ Lỗi Cơ sở dữ liệu: {err}")
        if 'conn' in locals() and conn.is_connected():
            conn.rollback() # Hoàn tác nếu có lỗi
            logging.info("Đã Rollback dữ liệu.")
    finally:
        if 'conn' in locals() and conn.is_connected():
            # Backup plan: Đảm bảo luôn bật lại khóa ngoại trước khi đóng
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")
            cursor.close()
            conn.close()
            logging.info("Đã đóng kết nối Database.")

if __name__ == "__main__":
    main()
