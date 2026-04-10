package com.weibo.controller;

import com.weibo.common.Result;
import com.weibo.entity.User;
import com.weibo.entity.Weibo;
import com.weibo.mapper.UserMapper;
import com.weibo.mapper.WeiboMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邮件测试接口（仅用于调试）
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestMailController {
    
    private final JavaMailSender mailSender;
    private final UserMapper userMapper;
    private final WeiboMapper weiboMapper;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    /**
     * 测试邮件发送
     */
    @GetMapping("/send-mail")
    public Result<?> testSendMail() {
        try {
            // 获取一个审核不通过的微博
            Weibo weibo = weiboMapper.selectById(21L);
            if (weibo == null) {
                return Result.error("未找到测试微博");
            }
            
            // 获取用户
            User user = userMapper.selectById(weibo.getUserId());
            if (user == null) {
                return Result.error("未找到用户");
            }
            
            log.info("准备发送邮件：to={}, from={}", user.getEmail(), mailUsername);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailUsername);
            helper.setTo(user.getEmail());
            helper.setSubject("【测试】微博审核未通过通知");
            
            String content = String.format(
                "亲爱的 %s：\n\n" +
                "这是一封测试邮件。\n\n" +
                "您发布的微博审核未通过。\n\n" +
                "微博内容：%s\n\n" +
                "未通过原因：%s\n\n" +
                "请修改后重新发布。\n\n" +
                "微博系统",
                user.getNickname() != null ? user.getNickname() : user.getUsername(),
                weibo.getContent(),
                weibo.getRemark()
            );
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("测试邮件发送成功：to={}", user.getEmail());
            return Result.success("邮件发送成功，请查收：" + user.getEmail());
        } catch (MessagingException e) {
            log.error("邮件发送失败", e);
            return Result.error("邮件发送失败：" + e.getMessage());
        }
    }
}
