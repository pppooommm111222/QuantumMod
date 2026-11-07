from setuptools import setup

setup(
    name='showlang',
    version='1.9',
    description='showlang',
    py_modules=['showlang'],
    # 这一行是魔法：告诉系统，生成一个叫 showlang 的命令，它去执行 showlang.py 里的 main 函数
    entry_points={
        'console_scripts': [
            'showlang=showlang:main',
        ],
    },
)
