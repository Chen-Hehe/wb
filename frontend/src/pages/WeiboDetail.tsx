import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Avatar, Button, Input, Space, message, Modal, Card } from 'antd';
import {
  UserOutlined,
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ShareAltOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { getCurrentUser } from '../api/auth';
import { getWeiboDetail, deleteWeibo, likeWeibo, unlikeWeibo, type Weibo } from '../api/weibo';
import { getWeiboComments, addComment, deleteComment, likeComment, unlikeComment, type Comment } from '../api/comment';
import { User } from '../api/user';
import './WeiboDetail.css';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { TextArea } = Input;

const WeiboDetail = () => {
  const { weiboId } = useParams<{ weiboId: string }>();
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [weibo, setWeibo] = useState<Weibo | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(false);
  const [commentContent, setCommentContent] = useState('');
  const [commenting, setCommenting] = useState(false);

  useEffect(() => {
    loadCurrentUser();
    loadWeiboDetail();
    loadComments();
  }, [weiboId]);

  const loadCurrentUser = async () => {
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  const loadWeiboDetail = async () => {
    setLoading(true);
    try {
      const data = await getWeiboDetail(Number(weiboId));
      setWeibo(data);
    } catch (error) {
      console.error('加载微博详情失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadComments = async () => {
    try {
      const data = await getWeiboComments(Number(weiboId));
      setComments(data.records);
    } catch (error) {
      console.error('加载评论失败:', error);
    }
  };

  const handleLike = async () => {
    if (!weibo) return;
    try {
      if (weibo.isLiked) {
        await unlikeWeibo(weibo.id);
      } else {
        await likeWeibo(weibo.id);
      }
      loadWeiboDetail();
    } catch (error) {
      console.error('点赞操作失败:', error);
    }
  };

  const handleAddComment = async () => {
    if (!commentContent.trim()) {
      message.warning('请输入评论内容');
      return;
    }

    setCommenting(true);
    try {
      await addComment({ weiboId: Number(weiboId), content: commentContent });
      message.success('评论成功');
      setCommentContent('');
      loadComments();
      loadWeiboDetail();
    } catch (error) {
      console.error('评论失败:', error);
    } finally {
      setCommenting(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      onOk: async () => {
        try {
          await deleteComment(commentId);
          message.success('删除成功');
          loadComments();
        } catch (error) {
          console.error('删除失败:', error);
        }
      },
    });
  };

  const handleLikeComment = async (commentId: number, isLiked: boolean) => {
    try {
      if (isLiked) {
        await unlikeComment(commentId);
      } else {
        await likeComment(commentId);
      }
      loadComments();
    } catch (error) {
      console.error('点赞评论失败:', error);
    }
  };

  const handleDeleteWeibo = () => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条微博吗？',
      onOk: async () => {
        try {
          await deleteWeibo(Number(weiboId));
          message.success('删除成功');
          navigate('/');
        } catch (error) {
          console.error('删除失败:', error);
        }
      },
    });
  };

  if (loading || !weibo) {
    return <div className="loading">加载中...</div>;
  }

  return (
    <div className="weibo-detail-page">
      {/* 微博详情 */}
      <Card className="weibo-detail-card" size="small">
        <div className="weibo-header">
          <Avatar
            src={weibo.user?.avatar}
            icon={<UserOutlined />}
            size={48}
            onClick={() => navigate(`/user/${weibo.userId}`)}
            style={{ cursor: 'pointer' }}
          />
          <div className="user-info">
            <div 
              className="user-nickname" 
              style={{ cursor: 'pointer' }}
              onClick={() => navigate(`/user/${weibo.userId}`)}
            >
              {weibo.user?.nickname}
            </div>
            <div className="user-username">@{weibo.user?.username}</div>
          </div>
          <div className="weibo-time">{dayjs(weibo.createdTime).format('YYYY-MM-DD HH:mm:ss')}</div>
          {currentUser?.id === weibo.userId && (
            <Button 
              type="text" 
              danger 
              icon={<DeleteOutlined />} 
              onClick={handleDeleteWeibo}
            >
              删除
            </Button>
          )}
        </div>

        <div className="weibo-content" style={{ marginTop: 16, fontSize: 16, lineHeight: 1.6 }}>
          {weibo.content}
        </div>

        {weibo.images && weibo.images.length > 0 && (
          <div className="weibo-images" style={{ display: 'flex', gap: '8px', marginTop: 16, flexWrap: 'wrap' }}>
            {weibo.images.map((img, index) => (
              <img 
                key={index} 
                src={img} 
                alt="" 
                className="weibo-image"
                style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 4, cursor: 'pointer' }}
              />
            ))}
          </div>
        )}

        <div className="weibo-stats" style={{ display: 'flex', gap: 24, marginTop: 16, paddingTop: 16, borderTop: '1px solid #f0f0f0', color: '#666' }}>
          <span>转发 {weibo.repostCount}</span>
          <span>评论 {weibo.commentCount}</span>
          <span>点赞 {weibo.likeCount}</span>
        </div>

        <div className="weibo-actions" style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16 }}>
          <Button
            icon={weibo.isLiked ? <LikeFilled /> : <LikeOutlined />}
            onClick={handleLike}
            className={weibo.isLiked ? 'liked' : ''}
          >
            点赞
          </Button>
          <Button icon={<MessageOutlined />}>评论</Button>
          <Button icon={<ShareAltOutlined />}>转发</Button>
        </div>
      </Card>

      {/* 评论区 */}
      <Card className="comment-section" size="small" style={{ marginTop: 16 }}>
        <div className="comment-title" style={{ fontSize: 16, fontWeight: 'bold', marginBottom: 16 }}>
          评论 ({comments.length})
        </div>

        {/* 发表评论 */}
        <div className="comment-input-box" style={{ marginBottom: 16 }}>
          <TextArea
            value={commentContent}
            onChange={(e) => setCommentContent(e.target.value)}
            placeholder="写下你的评论..."
            autoSize={{ minRows: 2, maxRows: 4 }}
            style={{ resize: 'none' }}
          />
          <div style={{ textAlign: 'right', marginTop: 8 }}>
            <Button type="primary" onClick={handleAddComment} loading={commenting}>
              评论
            </Button>
          </div>
        </div>

        {/* 评论列表 */}
        <div className="comment-list">
          {comments.map((comment) => (
            <div key={comment.id} className="comment-item" style={{ padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}>
              <div className="comment-header" style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
                <Avatar
                  src={comment.user?.avatar}
                  icon={<UserOutlined />}
                  size={32}
                  onClick={() => navigate(`/user/${comment.userId}`)}
                  style={{ cursor: 'pointer' }}
                />
                <div className="comment-user-info" style={{ flex: 1 }}>
                  <div 
                    className="comment-user-nickname" 
                    style={{ fontWeight: 500, cursor: 'pointer' }}
                    onClick={() => navigate(`/user/${comment.userId}`)}
                  >
                    {comment.user?.nickname}
                  </div>
                  <div className="comment-time" style={{ fontSize: 12, color: '#999' }}>
                    {dayjs(comment.createdTime).fromNow()}
                  </div>
                </div>
                {currentUser?.id === comment.userId && (
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleDeleteComment(comment.id)}
                  >
                    删除
                  </Button>
                )}
              </div>
              <div className="comment-content" style={{ marginLeft: 44, lineHeight: 1.5 }}>
                {comment.content}
              </div>
              <div className="comment-actions" style={{ marginLeft: 44, marginTop: 8 }}>
                <Button
                  type="text"
                  size="small"
                  icon={comment.isLiked ? <LikeFilled /> : <LikeOutlined />}
                  onClick={() => handleLikeComment(comment.id, !!comment.isLiked)}
                  className={comment.isLiked ? 'liked' : ''}
                >
                  {comment.likeCount}
                </Button>
              </div>
            </div>
          ))}

          {comments.length === 0 && (
            <div className="empty-comments" style={{ textAlign: 'center', padding: 40, color: '#999' }}>
              暂无评论，快来抢沙发吧！
            </div>
          )}
        </div>
      </Card>
    </div>
  );
};

export default WeiboDetail;
