// Lossless PNG color-format export. Pixel RGB values are never resampled or changed.
import zlib from 'node:zlib';
const signature=Buffer.from([137,80,78,71,13,10,26,10]);
function crc32(bytes){let crc=0xffffffff;for(const b of bytes){crc^=b;for(let i=0;i<8;i++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}return (crc^0xffffffff)>>>0;}
function chunk(type,data){const name=Buffer.from(type);const out=Buffer.alloc(12+data.length);out.writeUInt32BE(data.length);name.copy(out,4);data.copy(out,8);out.writeUInt32BE(crc32(Buffer.concat([name,data])),8+data.length);return out;}
function paeth(a,b,c){const p=a+b-c,pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);return pa<=pb&&pa<=pc?a:pb<=pc?b:c;}
export function rgbPixels(png){
  if(!png.subarray(0,8).equals(signature))throw new Error('Not PNG');
  let header;const parts=[];
  for(let pos=8;pos<png.length;){const len=png.readUInt32BE(pos),type=png.subarray(pos+4,pos+8).toString();const data=png.subarray(pos+8,pos+8+len);if(type==='IHDR')header=Buffer.from(data);if(type==='IDAT')parts.push(data);pos+=len+12;}
  const w=header.readUInt32BE(0),h=header.readUInt32BE(4),type=header[9];
  if(header[8]!==8||header[12]!==0||![2,6].includes(type))throw new Error('Expected non-interlaced 8bit RGB/RGBA PNG');
  const bpp=type===6?4:3,stride=w*bpp,raw=zlib.inflateSync(Buffer.concat(parts)),decoded=Buffer.alloc(stride*h);
  let read=0;
  for(let y=0;y<h;y++){
    const filter=raw[read++];if(filter>4)throw new Error('Unknown PNG filter');
    for(let x=0;x<stride;x++){
      const a=x>=bpp?decoded[y*stride+x-bpp]:0,b=y?decoded[(y-1)*stride+x]:0,c=y&&x>=bpp?decoded[(y-1)*stride+x-bpp]:0;
      const predictor=filter===0?0:filter===1?a:filter===2?b:filter===3?Math.floor((a+b)/2):paeth(a,b,c);
      decoded[y*stride+x]=(raw[read++]+predictor)&255;
    }
  }
  const rgb=Buffer.alloc(w*h*3);
  for(let i=0;i<w*h;i++){if(bpp===4&&decoded[i*4+3]!==255)throw new Error('Cannot discard nonopaque alpha');rgb[i*3]=decoded[i*bpp];rgb[i*3+1]=decoded[i*bpp+1];rgb[i*3+2]=decoded[i*bpp+2];}
  return {width:w,height:h,rgb,header};
}
export function png24(png){
  const {width,height,rgb,header}=rgbPixels(png);header[9]=2;
  const rows=Buffer.alloc((width*3+1)*height);
  for(let y=0;y<height;y++)rgb.copy(rows,y*(width*3+1)+1,y*width*3,(y+1)*width*3);
  return Buffer.concat([signature,chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(rows,{level:9})),chunk('IEND',Buffer.alloc(0))]);
}
