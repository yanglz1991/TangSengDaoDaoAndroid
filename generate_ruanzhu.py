# -*- coding: utf-8 -*-
import os
import re
import sys
import subprocess

# 自动安装 reportlab 库
try:
    import reportlab
except ImportError:
    print("Installing reportlab...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "reportlab"])
    except Exception as e:
        print(f"Error installing reportlab via pip: {e}. Trying to install via pip3...")
        subprocess.check_call(["pip3", "install", "reportlab"])

from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

def collect_code(root_dir, target_lines_count=3000):
    """
    收集项目中的 Kotlin 和 Java 源代码，过滤空行和注释，直到收集满指定的行数。
    """
    code_lines = []
    # 需要扫描的子目录（核心自主模块）
    modules = ['app', 'wkbase', 'wkuikit', 'wklogin', 'wkgroupmanage', 'wkvideo', 'wkfile']
    
    # 构建绝对路径列表
    scan_dirs = []
    for m in modules:
        path = os.path.join(root_dir, m)
        if os.path.exists(path):
            scan_dirs.append(path)
            
    if not scan_dirs:
        # 如果没有找到具体模块，就扫描整个根目录（排除 build, .git, .gradle, MyLibs 等）
        scan_dirs = [root_dir]

    exclude_dirs = {'build', '.git', '.gradle', '.idea', 'MyLibs', 'gradle'}
    
    in_block_comment = False
    
    for scan_dir in scan_dirs:
        if len(code_lines) >= target_lines_count:
            break
            
        for dirpath, dirnames, filenames in os.walk(scan_dir):
            # 过滤掉排除目录
            dirnames[:] = [d for d in dirnames if d not in exclude_dirs and not d.startswith('.')]
            
            if len(code_lines) >= target_lines_count:
                break
                
            for filename in filenames:
                if len(code_lines) >= target_lines_count:
                    break
                    
                if filename.endswith('.kt') or filename.endswith('.java'):
                    filepath = os.path.join(dirpath, filename)
                    try:
                        with open(filepath, 'r', encoding='utf-8') as f:
                            lines = f.readlines()
                    except UnicodeDecodeError:
                        # 尝试使用 gbk 编码读取
                        try:
                            with open(filepath, 'r', encoding='gbk') as f:
                                lines = f.readlines()
                        except Exception:
                            continue
                    except Exception:
                        continue
                        
                    for line in lines:
                        if len(code_lines) >= target_lines_count:
                            break
                            
                        stripped = line.strip()
                        
                        # 处理多行注释
                        if in_block_comment:
                            if '*/' in stripped:
                                in_block_comment = False
                                # 如果同行还有代码在 */ 之后，可保留，但软著没必要搞太复杂，直接跳过该行
                            continue
                        else:
                            if stripped.startswith('/*'):
                                if '*/' not in stripped:
                                    in_block_comment = True
                                continue
                                
                        # 过滤空行、单行注释、导包、注解或大括号等无实质内容行（软著代码质量要求高，过滤掉大括号行和无意义行）
                        if not stripped:
                            continue
                        if stripped.startswith('//'):
                            continue
                        if stripped.startswith('*') or stripped.startswith('import ') or stripped.startswith('package '):
                            continue
                        if stripped == '{' or stripped == '}' or stripped == '};':
                            continue
                            
                        # 简单清除行尾单行注释
                        if '//' in line:
                            # 确保 // 不是在字符串中
                            parts = line.split('//')
                            clean_line = parts[0].rstrip()
                            if not clean_line.strip():
                                continue
                        else:
                            clean_line = line.rstrip()
                            
                        code_lines.append(clean_line)
                        
    # 截取前 3000 行
    return code_lines[:target_lines_count]

def generate_pdf(code_lines, pdf_path, app_name_header="禧语安卓 APP V1.0"):
    """
    使用 ReportLab 生成 PDF 文件，共 60 页，每页 50 行。
    """
    # A4 尺寸：595.27 x 841.89 磅
    c = canvas.Canvas(pdf_path, pagesize=A4)
    width, height = A4
    
    # 注册中文字体（使用 macOS 自带的华文细黑，支持中文和英文，效果完美且不依赖外部资源）
    pdfmetrics.registerFont(TTFont('STHeiti-Light', '/System/Library/Fonts/STHeiti Light.ttc'))
    c.setFont('STHeiti-Light', 9)
    
    lines_per_page = 50
    total_pages = 60
    
    # 每一行的纵向间距设置
    # A4 高 841.89。页眉在 800 处，页脚在 40 处。
    # 我们留 760 磅为起始行高，每行高 13.8 磅，50 行大约需要 13.8 * 49 = 676.2 磅。
    # 760 - 676.2 = 83.8 磅，刚好在页脚之上。
    y_start = 760
    line_height = 13.8
    
    for page_num in range(total_pages):
        page_idx = page_num + 1
        
        # 1. 绘制页眉
        c.setFont('STHeiti-Light', 9)
        c.drawString(50, 805, app_name_header)
        # 页眉分割线
        c.setLineWidth(0.5)
        c.line(50, 798, width - 50, 798)
        
        # 2. 绘制页脚
        c.setFont('STHeiti-Light', 9)
        page_str = f"第 {page_idx} 页 / 共 {total_pages} 页"
        c.drawCentredString(width / 2, 40, page_str)
        
        # 3. 绘制本页的 50 行代码
        start_line = page_num * lines_per_page
        end_line = start_line + lines_per_page
        page_lines = code_lines[start_line:end_line]
        
        c.setFont('STHeiti-Light', 9)
        for i, line_content in enumerate(page_lines):
            # 将制表符 tab 替换为空格，避免显示错乱
            display_line = line_content.replace('\t', '    ')
            # 限制单行长度，防止超出页面边缘。80个字符比较合适
            if len(display_line) > 85:
                display_line = display_line[:82] + "..."
                
            y_pos = y_start - (i * line_height)
            # 50 为左边距
            c.drawString(50, y_pos, display_line)
            
        c.showPage()
        
    c.save()
    print(f"Successfully generated PDF: {pdf_path}")

if __name__ == '__main__':
    root_directory = os.path.dirname(os.path.abspath(__file__))
    txt_output_path = os.path.join(root_directory, "ruanzhu_source.txt")
    pdf_output_path = os.path.join(root_directory, "ruanzhu_source.pdf")
    
    print("Collecting 3000 lines of code...")
    collected_code = collect_code(root_directory, 3000)
    
    actual_count = len(collected_code)
    print(f"Collected {actual_count} lines of code.")
    
    # 如果代码不足 3000 行，则用循环填充，确保刚好 3000 行
    if actual_count < 3000:
        print(f"Warning: Only collected {actual_count} lines. Duplicating code to reach 3000 lines...")
        while len(collected_code) < 3000:
            collected_code.extend(collected_code[:3000 - len(collected_code)])
            
    # 写入到源码 txt 文件中
    with open(txt_output_path, 'w', encoding='utf-8') as f:
        for line in collected_code:
            f.write(line + "\n")
    print(f"Successfully wrote code to: {txt_output_path}")
    
    # 生成 PDF
    print("Generating PDF...")
    generate_pdf(collected_code, pdf_output_path, "禧语安卓 APP V1.0")
