import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Dropdown, Space, Button, Input, message, Modal, Card, Upload, Image } from 'antd';
import {
  MoreOutlined,
  DeleteOutlined,
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ShareAltOutlined,
  SendOutlined,
  UserOutlined,
  PictureOutlined,
  CloseOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { getCurrentUser } from '../api/auth';
import { getWeiboList, deleteWeibo, likeWeibo, unlikeWeibo, publishWeibo, type Weibo } from '../api/weibo';
import { getUserInfo, type User } from '../api/user';
import { uploadImage } from '../api/upload';
import { getImageUrl } from '../config';
import request from '../utils/request';
import './Home.css';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { TextArea } = Input;

const Home = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [weibos, setWeibos] = useState<Weibo[]>([]);
  const [loading, setLoading] = useState(false);
  const [publishContent, setPublishContent] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadFiles, setUploadFiles] = useState<UploadFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadCurrentUser();
    loadWeibos();
  }, []);

  const loadCurrentUser = async () => {
    // 直接从 localStorage 读取用户信息
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      console.log('从 localStorage 加载用户:', user);
      setCurrentUser(user);
      return;
    }
    
    // 如果没有缓存，尝试从 API 获取
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const user = await request.get('/users/current');
        console.log('从 API 获取用户:', user);
        setCurrentUser(user);
        localStorage.setItem('user', JSON.stringify(user));
      } catch (error) {
        console.error('获取用户信息失败:', error);
      }
    }
  };

  const loadWeibos = async () => {
    setLoading(true);
    try {
      const data = await getWeiboList();
      console.log('微博列表数据:', data);
      const weibosData = data?.records || data?.list || [];
      console.log('解析后的微博数据:', weibosData);
      if (weibosData.length > 0) {
        console.log('第一条微博的 images:', weibosData[0].images);
      }
      setWeibos(weibosData);
    } catch (error) {
      console.error('加载微博失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    if (!publishContent.trim() && uploadFiles.length === 0) {
      message.warning('请输入微博内容或上传图片');
      return;
    }

    setPublishing(true);
    try {
      // 上传图片
      let imageUrls: string[] = [];
      if (uploadFiles.length > 0) {
        setUploading(true);
        const uploadPromises = uploadFiles.map((file) => uploadImage(file.originFileObj!));
        const results = await Promise.all(uploadPromises);
        imageUrls = results.map((res: any) => res.imgUrl);
        setUploading(false);
      }

      // 发布微博
      await publishWeibo({
        title: '新鲜事',
        content: publishContent,
        images: imageUrls.length > 0 ? imageUrls : undefined,
      });
      
      message.success('发布成功');
      setPublishContent('');
      setUploadFiles([]);
      loadWeibos();
    } catch (error: any) {
      console.error('发布失败:', error);
      setUploading(false);
    } finally {
      setPublishing(false);
    }
  };

  const handleFileChange = (file: File) => {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      message.error('只能上传图片文件');
      return false;
    }
    
    // 验证文件大小 (5MB)
    if (file.size > 5 * 1024 * 1024) {
      message.error('图片大小不能超过 5MB');
      return false;
    }
    
    return true;
  };

  const handleRemoveImage = (index: number) => {
    setUploadFiles(uploadFiles.filter((_, i) => i !== index));
  };

  const handleLike = async (weiboId: number, isLiked: boolean) => {
    try {
      if (isLiked) {
        await unlikeWeibo(weiboId);
      } else {
        await likeWeibo(weiboId);
      }
      loadWeibos();
    } catch (error) {
      console.error('点赞操作失败:', error);
    }
  };

  const handleDelete = (weiboId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条微博吗？',
      onOk: async () => {
        try {
          await deleteWeibo(weiboId);
          message.success('删除成功');
          loadWeibos();
        } catch (error) {
          console.error('删除失败:', error);
        }
      },
    });
  };

  const weiboMenu = (weibo: Weibo) => {
    const canDelete = currentUser?.id === weibo.userId;
    console.log('微博 ID:', weibo.id, '当前用户 ID:', currentUser?.id, '微博用户 ID:', weibo.userId, '可否删除:', canDelete);
    return {
      items: canDelete
        ? [{ key: 'delete', icon: <DeleteOutlined />, label: '删除', danger: true, onClick: () => handleDelete(weibo.id) }]
        : [],
    };
  };

  return (
    <div className="home-container">
      {/* 发布框 */}
      <Card className="publish-box" size="small">
        <div style={{ display: 'flex', gap: '12px' }}>
          <Avatar 
            src={currentUser?.avatar} 
            icon={<UserOutlined />} 
            size={48}
            onClick={() => navigate(`/user/${currentUser?.id}`)}
            style={{ cursor: 'pointer' }}
          />
          <div style={{ flex: 1 }}>
            <TextArea
              value={publishContent}
              onChange={(e) => setPublishContent(e.target.value)}
              placeholder="有什么新鲜事想和大家分享？"
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{ resize: 'none' }}
            />
            
            {/* 图片预览 */}
            {uploadFiles.length > 0 && (
              <div style={{ display: 'flex', gap: '8px', marginTop: 12, flexWrap: 'wrap' }}>
                {uploadFiles.map((file, index) => (
                  <div key={index} style={{ position: 'relative', display: 'inline-block' }}>
                    <Image
                      src={URL.createObjectURL(file.originFileObj!)}
                      alt={file.name}
                      style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 4 }}
                    />
                    <Button
                      type="text"
                      size="small"
                      icon={<CloseOutlined />}
                      onClick={() => handleRemoveImage(index)}
                      style={{
                        position: 'absolute',
                        top: -8,
                        right: -8,
                        padding: 0,
                        width: 20,
                        height: 20,
                        background: 'rgba(0,0,0,0.5)',
                        color: '#fff',
                        borderRadius: '50%',
                      }}
                    />
                  </div>
                ))}
              </div>
            )}
            
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file && handleFileChange(file)) {
                    setUploadFiles([...uploadFiles, { uid: Date.now(), name: file.name, originFileObj: file }]);
                  }
                  if (fileInputRef.current) {
                    fileInputRef.current.value = '';
                  }
                }}
              />
              <Button 
                icon={<PictureOutlined />} 
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadFiles.length >= 9}
              >
                图片 {uploadFiles.length > 0 && `(${uploadFiles.length}/9)`}
              </Button>
              <Button 
                type="primary" 
                icon={<SendOutlined />} 
                onClick={handlePublish} 
                loading={publishing || uploading}
              >
                发布
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {/* 微博列表 */}
      <div className="weibo-list">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px' }}>加载中...</div>
        ) : weibos.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无微博，快来发布第一条吧！
          </div>
        ) : (
          weibos.map((weibo) => (
            <Card key={weibo.id} className="weibo-card" size="small">
              <div className="weibo-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                  <Avatar
                    src={weibo.user?.avatar}
                    icon={<UserOutlined />}
                    size={48}
                    onClick={() => navigate(`/user/${weibo.userId}`)}
                    style={{ cursor: 'pointer' }}
                  />
                  <div>
                    <div 
                      className="user-nickname" 
                      style={{ fontWeight: 'bold', cursor: 'pointer' }}
                      onClick={() => navigate(`/user/${weibo.userId}`)}
                    >
                      {weibo.user?.nickname}
                    </div>
                    <div className="user-username" style={{ color: '#999', fontSize: '12px' }}>
                      @{weibo.user?.username}
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span style={{ color: '#999', fontSize: '12px' }}>
                    {dayjs(weibo.createdTime).fromNow()}
                  </span>
                  <Dropdown menu={weiboMenu(weibo)} trigger={['click']} placement="bottomRight">
                    <Button type="text" icon={<MoreOutlined />} />
                  </Dropdown>
                </div>
              </div>

              <div className="weibo-content" style={{ marginTop: 12, lineHeight: 1.6 }}>
                {weibo.content}
              </div>

              {weibo.images && weibo.images.length > 0 && (
                <div className="weibo-images" style={{ display: 'flex', gap: '8px', marginTop: 12, flexWrap: 'wrap' }}>
                  {weibo.images.map((img: string, index: number) => {
                    const imgUrl = getImageUrl(img);
                    console.log(`微博 ${weibo.id} 图片 ${index}:`, img, '=>', imgUrl);
                    return (
                      <Image
                        key={index} 
                        src={imgUrl} 
                        alt="" 
                        className="weibo-image"
                        style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 4, cursor: 'pointer' }}
                        preview={{
                          src: imgUrl,
                        }}
                        fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWNgYGBgAAAABQABh6FO1AAAAABJRU5ErkJggg=="
                      />
                    );
                  })}
                </div>
              )}

              <div className="weibo-footer" style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
                <div
                  className={`weibo-action ${weibo.isLiked ? 'liked' : ''}`}
                  onClick={() => handleLike(weibo.id, !!weibo.isLiked)}
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: weibo.isLiked ? '#ff4d4f' : '#666' }}
                >
                  {weibo.isLiked ? <LikeFilled /> : <LikeOutlined />}
                  <span>{weibo.likeCount}</span>
                </div>
                <div 
                  className="weibo-action" 
                  onClick={() => navigate(`/weibo/${weibo.id}`)}
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: '#666' }}
                >
                  <MessageOutlined />
                  <span>{weibo.commentCount}</span>
                </div>
                <div 
                  className="weibo-action"
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: '#666' }}
                >
                  <ShareAltOutlined />
                  <span>{weibo.repostCount}</span>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default Home;
