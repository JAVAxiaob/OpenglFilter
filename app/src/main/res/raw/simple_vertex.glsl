// 顶点着色器  顶点坐标 决定  顶点的位置  屏幕坐标
attribute vec4 vPosition;

void main(){
// 内置变量： 把坐标点赋值给gl_position 就Ok了。 固定赋值位置 ，全部覆盖
    gl_Position = vPosition;
}



