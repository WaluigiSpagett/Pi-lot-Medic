import sys
from PyQt5.QtWidgets import (
    QApplication, QWidget, QLabel, QLineEdit, QPushButton, QVBoxLayout,
    QListWidget, QMessageBox, QStackedWidget, QListWidgetItem, QGraphicsDropShadowEffect, QHBoxLayout, QFrame
)
from PyQt5.QtGui import QFont, QPalette, QColor, QPixmap
from PyQt5.QtCore import Qt

# Placeholder plane data
planes = {
    "192.168.1.10": "Plane Alpha",
    "192.168.1.11": "Plane Beta",
    "192.168.1.12": "Plane Gamma"
}

def apply_shadow(widget):
    shadow = QGraphicsDropShadowEffect()
    shadow.setBlurRadius(20)
    shadow.setXOffset(0)
    shadow.setYOffset(4)
    shadow.setColor(QColor(0, 0, 0, 160))
    widget.setGraphicsEffect(shadow)

class InputScreen(QWidget):
    def __init__(self, stacked_widget):
        super().__init__()
        self.stacked_widget = stacked_widget
        self.init_ui()

    def init_ui(self):
        layout = QVBoxLayout()
        layout.setSpacing(20)
        layout.setAlignment(Qt.AlignCenter)

        title = QLabel("Confirm Delivery Details")
        title.setFont(QFont("Inter", 24, QFont.Bold))
        title.setStyleSheet("color: #FFFFFF;")
        title.setAlignment(Qt.AlignCenter)
        layout.addWidget(title)

        self.medicine_input = QLineEdit("Paracetamol")
        self.medicine_input.setPlaceholderText("Enter Medicine Name")
        self.medicine_input.setStyleSheet("padding: 12px; border-radius: 12px; border: 2px solid #555; background-color: #2C2C2C; color: #FFFFFF;")
        apply_shadow(self.medicine_input)

        self.waypoint_input = QLineEdit("51.5074,-0.1278,100")
        self.waypoint_input.setPlaceholderText("lat,lon,alt")
        self.waypoint_input.setStyleSheet("padding: 12px; border-radius: 12px; border: 2px solid #555; background-color: #2C2C2C; color: #FFFFFF;")
        apply_shadow(self.waypoint_input)

        next_button = QPushButton("Continue")
        next_button.setStyleSheet("""
            QPushButton {
                background-color: #2E7D32; color: white; padding: 12px;
                border-radius: 20px; font-weight: bold;
                font-family: 'Inter';
            }
            QPushButton:hover {
                background-color: #1B5E20;
            }
        """)
        apply_shadow(next_button)
        next_button.clicked.connect(self.validate_inputs)

        layout.addWidget(self.medicine_input)
        layout.addWidget(self.waypoint_input)
        layout.addWidget(next_button)

        self.setLayout(layout)

    def validate_inputs(self):
        if self.medicine_input.text() and self.waypoint_input.text():
            self.stacked_widget.setCurrentIndex(1)
        else:
            QMessageBox.warning(self, "Validation Error", "Please ensure all fields are filled.")


class PlaneSelectionScreen(QWidget):
    def __init__(self, stacked_widget):
        super().__init__()
        self.stacked_widget = stacked_widget
        self.init_ui()

    def init_ui(self):
        layout = QVBoxLayout()
        layout.setSpacing(20)
        layout.setAlignment(Qt.AlignCenter)

        title = QLabel("Select Delivery Plane")
        title.setFont(QFont("Inter", 24, QFont.Bold))
        title.setStyleSheet("color: #FFFFFF;")
        title.setAlignment(Qt.AlignCenter)
        layout.addWidget(title)

        self.plane_list = QListWidget()
        self.plane_list.setStyleSheet("""
            QListWidget {
                border: none;
                background-color: #1E1E1E;
                color: #FFFFFF;
            }
            QListWidget::item {
                padding: 16px;
                border-radius: 12px;
                background-color: #2C2C2C;
                margin: 10px;
            }
            QListWidget::item:selected {
                background-color: #424242;
            }
        """)
        for ip, name in planes.items():
            item_widget = QWidget()
            item_layout = QHBoxLayout()
            item_layout.setContentsMargins(10, 10, 10, 10)
            item_layout.setSpacing(15)

            status_indicator = QFrame()
            status_indicator.setFixedSize(14, 14)
            status_indicator.setStyleSheet("background-color: #2E7D32; border-radius: 7px;")

            plane_label = QLabel(f"{name}\n{ip}")
            plane_label.setFont(QFont("Inter", 16))
            plane_label.setStyleSheet("color: #FFFFFF;")

            item_layout.addWidget(status_indicator)
            item_layout.addWidget(plane_label)
            item_widget.setLayout(item_layout)

            list_item = QListWidgetItem()
            list_item.setSizeHint(item_widget.sizeHint())

            self.plane_list.addItem(list_item)
            self.plane_list.setItemWidget(list_item, item_widget)

        layout.addWidget(self.plane_list)

        send_button = QPushButton("Send Waypoint")
        send_button.setStyleSheet("""
            QPushButton {
                background-color: #2E7D32; color: white; padding: 12px;
                border-radius: 20px; font-weight: bold;
                font-family: 'Inter';
            }
            QPushButton:hover {
                background-color: #1B5E20;
            }
        """)
        apply_shadow(send_button)
        send_button.clicked.connect(self.send_waypoint)
        layout.addWidget(send_button)

        self.setLayout(layout)

    def send_waypoint(self):
        selected_items = self.plane_list.selectedItems()
        if not selected_items:
            QMessageBox.warning(self, "Selection Error", "Please select a plane to send the waypoint to.")
            return

        selected_plane = selected_items[0].text()
        QMessageBox.information(self, "Success", f"Waypoint sent to {selected_plane}.")


class MainApp(QStackedWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Medicine Delivery System")
        self.setGeometry(100, 100, 500, 450)
        self.setStyle()

        self.input_screen = InputScreen(self)
        self.plane_screen = PlaneSelectionScreen(self)

        self.addWidget(self.input_screen)
        self.addWidget(self.plane_screen)

    def setStyle(self):
        palette = self.palette()
        palette.setColor(QPalette.Window, QColor("#121212"))
        self.setPalette(palette)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = MainApp()
    window.show()
    sys.exit(app.exec_())
