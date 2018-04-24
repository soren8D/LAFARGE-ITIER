#pragma  version (1);
#pragma  rs  java_package_name(com.example.yitier910e.myapplication2);

uchar4  RS_KERNEL  toSepia(uchar4  in) {
float4  pixelf = rsUnpackColor8888(in);
float  r = (0.393* pixelf.r+ 0.769* pixelf.g+ 0.189* pixelf.b);
float  g = (0.349* pixelf.r+ 0.686* pixelf.g+ 0.168* pixelf.b);
float  b = (0.272* pixelf.r+ 0.534* pixelf.g+ 0.131* pixelf.b);
return  rsPackColorTo8888(r , g , b , pixelf.a);
}
