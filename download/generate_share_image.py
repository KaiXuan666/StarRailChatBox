import sys
import subprocess
import os
import random
import math

# 自动安装依赖
try:
    from PIL import Image, ImageDraw, ImageFont, ImageFilter
    import qrcode
except ImportError:
    print("正在安装所需的 PIL 和 qrcode 库...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "qrcode[pil]", "Pillow"])
    from PIL import Image, ImageDraw, ImageFont, ImageFilter
    import qrcode

def generate_share_card():
    # 尺寸设定 (接近 9:16)
    width = 720
    height = 1280
    
    # 1. 创建基础图像 (RGBA)
    image = Image.new("RGBA", (width, height), (0, 0, 0, 255))
    draw = ImageDraw.Draw(image)
    
    # 2. 绘制深邃星空渐变背景
    for y in range(height):
        # 顶端 #0d1222 到 底端 #05070a 的平滑渐变
        factor = y / height
        r = int(13 + (5 - 13) * factor)
        g = int(18 + (7 - 18) * factor)
        b = int(34 + (10 - 34) * factor)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))
        
    # 3. 绘制星云极光 (利用模糊)
    nebula = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    nebula_draw = ImageDraw.Draw(nebula)
    # 画几个彩色大圆和椭圆
    nebula_draw.ellipse([-200, -200, 500, 500], fill=(79, 195, 247, 25)) # 亮蓝色星云
    nebula_draw.ellipse([300, 400, 900, 1000], fill=(147, 51, 234, 15))  # 紫色星云
    nebula_draw.ellipse([100, 800, 800, 1500], fill=(236, 196, 133, 10)) # 金色星光带
    # 进行高斯模糊
    nebula = nebula.filter(ImageFilter.GaussianBlur(100))
    image = Image.alpha_composite(image, nebula)
    draw = ImageDraw.Draw(image)
    
    # 4. 绘制漫天繁星
    random.seed(42) # 固定随机种子，使生成图稳定
    for _ in range(120):
        x = random.randint(0, width)
        y = random.randint(0, height)
        size = random.choice([1, 1, 2, 3])
        opacity = random.randint(50, 220)
        draw.ellipse([(x, y), (x + size, y + size)], fill=(255, 255, 255, opacity))
        
    # 5. 加载字体 (微软雅黑)
    # Windows 默认字体路径，若不存在则回退默认
    font_bold_path = "C:/Windows/Fonts/msyhbd.ttc"
    font_reg_path = "C:/Windows/Fonts/msyh.ttc"
    
    if not os.path.exists(font_bold_path):
        font_bold_path = "arial.ttf" # 兜底
    if not os.path.exists(font_reg_path):
        font_reg_path = "arial.ttf"
        
    try:
        title_font = ImageFont.truetype(font_bold_path, 42)
        subtitle_font = ImageFont.truetype(font_reg_path, 22)
        card_title_font = ImageFont.truetype(font_bold_path, 24)
        body_font = ImageFont.truetype(font_reg_path, 20)
        bottom_title_font = ImageFont.truetype(font_bold_path, 22)
        bottom_desc_font = ImageFont.truetype(font_reg_path, 16)
    except IOError:
        title_font = subtitle_font = card_title_font = body_font = bottom_title_font = bottom_desc_font = ImageFont.load_default()

    # 6. 绘制开拓之星徽章 (手绘多边形)
    # 绘制星铁经典的四角星
    center_x, center_y = 360, 150
    # 外部光环
    draw.ellipse([(center_x - 45, center_y - 45), (center_x + 45, center_y + 45)], outline=(79, 195, 247, 80), width=2)
    # 绘制闪耀四角星 (X轴长, Y轴长)
    def get_star_points(cx, cy, R, r):
        points = []
        for i in range(8):
            angle = i * (3.14159 / 4)
            dist = R if i % 2 == 0 else r
            points.append((cx + dist * math.cos(angle), cy + dist * math.sin(angle)))
        return points
    
    star_pts = get_star_points(center_x, center_y, 35, 10)
    draw.polygon(star_pts, fill=(236, 196, 133, 255)) # 金色实心星
    
    # 7. 绘制文字标题
    # 主标题
    title_text = "崩铁ChatBox Android 版"
    t_w = draw.textlength(title_text, font=title_font)
    draw.text(( (width - t_w)//2, 220 ), title_text, fill=(236, 196, 133, 255), font=title_font)
    
    # 副标题
    sub_text = "✦ 开拓意志的跨平台智能 AI 聊天盒 ✦"
    s_w = draw.textlength(sub_text, font=subtitle_font)
    draw.text(( (width - s_w)//2, 285 ), sub_text, fill=(138, 156, 174, 255), font=subtitle_font)

    # 8. 绘制主体卡片 (毛玻璃背景 + 金色切角)
    card_left = 60
    card_top = 350
    card_right = 660
    card_bottom = 880
    
    # 用半透明深色填充卡片
    draw.rectangle([card_left, card_top, card_right, card_bottom], fill=(15, 20, 31, 190), outline=(255, 255, 255, 20))
    
    # 绘制四角发光棱角角标 (星铁经典设计)
    corner_size = 15
    # 左上角角标
    draw.line([(card_left - 2, card_top - 2), (card_left + corner_size, card_top - 2)], fill=(236, 196, 133, 255), width=3)
    draw.line([(card_left - 2, card_top - 2), (card_left - 2, card_top + corner_size)], fill=(236, 196, 133, 255), width=3)
    # 右上角角标
    draw.line([(card_right + 2, card_top - 2), (card_right - corner_size, card_top - 2)], fill=(236, 196, 133, 255), width=3)
    draw.line([(card_right + 2, card_top - 2), (card_right + 2, card_top + corner_size)], fill=(236, 196, 133, 255), width=3)
    # 左下角角标
    draw.line([(card_left - 2, card_bottom + 2), (card_left + corner_size, card_bottom + 2)], fill=(236, 196, 133, 255), width=3)
    draw.line([(card_left - 2, card_bottom + 2), (card_left - 2, card_bottom - corner_size)], fill=(236, 196, 133, 255), width=3)
    # 右下角角标
    draw.line([(card_right + 2, card_bottom + 2), (card_right - corner_size, card_bottom + 2)], fill=(236, 196, 133, 255), width=3)
    draw.line([(card_right + 2, card_bottom + 2), (card_right + 2, card_bottom - corner_size)], fill=(236, 196, 133, 255), width=3)
    
    # 装饰线
    draw.line([(card_left + 30, card_top + 50), (card_right - 30, card_top + 50)], fill=(79, 195, 247, 80), width=1)
    # 卡片内部标题
    card_title = "核心功能与特色"
    ct_w = draw.textlength(card_title, font=card_title_font)
    draw.text(( (width - ct_w)//2, card_top + 15 ), card_title, fill=(79, 195, 247, 255), font=card_title_font)

    # 9. 写入特色描述
    descriptions = [
        "不止崩铁，可创建任意角色",
        "多模态智能对话，支持图片、语音发送接收",
        "多角色多会话，无缝切换并尝试不同可能",
        "支持快捷回复、音色克隆等丰富功能",
        "角色工坊：可快捷导入他人分享的角色卡",
        "数据备份与导出，PC和手机继承相同数据"
    ]
    
    start_y = card_top + 75
    line_spacing = 75
    
    for i, desc in enumerate(descriptions):
        y_pos = start_y + i * line_spacing
        # 绘制金色四角星符号 ✦
        draw.text((card_left + 30, y_pos), "✦", fill=(236, 196, 133, 255), font=body_font)
        # 绘制文本
        draw.text((card_left + 60, y_pos), desc, fill=(241, 245, 249, 255), font=body_font)

    # 10. 生成真实的二维码并贴在右下角
    qr_url = "https://oss.qyaichat.com/public/update/index.html"
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=2,
    )
    qr.add_data(qr_url)
    qr.make(fit=True)
    
    # 为了搭配崩铁风格，将二维码的前景色改为深蓝色，背景色改为淡金白色
    qr_img = qr.make_image(fill_color="#0d1b2a", back_color="#fcf6ec").convert("RGBA")
    # 缩放至 200x200
    qr_size = 200
    qr_img = qr_img.resize((qr_size, qr_size), Image.Resampling.LANCZOS)
    
    # 绘制二维码边框
    qr_x = 440
    qr_y = 960
    # 在底图上绘制二维码贴图
    image.paste(qr_img, (qr_x, qr_y), qr_img)
    draw.rectangle([qr_x - 2, qr_y - 2, qr_x + qr_size + 1, qr_y + qr_size + 1], outline=(236, 196, 133, 180), width=2)
    
    # 11. 绘制左侧二维码说明文字
    desc_x = 80
    desc_y = 1010
    draw.text((desc_x, desc_y), "长按识别二维码", fill=(236, 196, 133, 255), font=bottom_title_font)
    draw.text((desc_x, desc_y + 40), "立即下载并开启您的开拓之旅", fill=(241, 245, 249, 230), font=bottom_desc_font)
    draw.text((desc_x, desc_y + 75), "—— 目标，星辰大海！——", fill=(138, 156, 174, 255), font=bottom_desc_font)

    # 绘制底部的点缀装饰线
    draw.line([(60, 1210), (width - 60, 1210)], fill=(79, 195, 247, 50), width=1)
    
    # 12. 保存图像
    output_path = os.path.join(os.path.dirname(__file__), "StarRailChatBox_Share.png")
    image.save(output_path, "PNG")
    print(f"海报生成完毕，已保存至: {output_path}")

if __name__ == "__main__":
    generate_share_card()
