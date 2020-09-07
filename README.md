# AgoraYUVCamera

## 描述
部分Iot设备或者摄像头数据特殊时，自采集会模糊，可以用camera回调数据本地预览看一下，本地预览正常的情况直接推camera的YUV数据即可

## 本demo实现注意事项
- setClientRole(1)，设置主播模式进入，对端观众端拉流即可看到主播数据
- demo本地预览画质没有做适配，看起来可能存在变形，请自行修改UI(SurfaceView宽高)，远端正常即可