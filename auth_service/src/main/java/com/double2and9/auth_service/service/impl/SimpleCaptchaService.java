package com.double2and9.auth_service.service.impl;

import com.double2and9.auth_service.dto.response.CaptchaDTO;
import com.double2and9.auth_service.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 简单图形验证码服务实现
 */
@Service
@RequiredArgsConstructor
public class SimpleCaptchaService implements CaptchaService {
    
    private final StringRedisTemplate redisTemplate;
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int CAPTCHA_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;
    
    @Override
    public CaptchaDTO generateCaptcha() {
        // 生成随机验证码
        String captchaCode = generateRandomCode(CAPTCHA_LENGTH);
        String captchaId = UUID.randomUUID().toString();
        
        // 存储到Redis，设置过期时间
        redisTemplate.opsForValue().set(
            CAPTCHA_PREFIX + captchaId, 
            captchaCode,
            Duration.ofMinutes(CAPTCHA_EXPIRE_MINUTES)
        );
        
        // 生成验证码图片
        BufferedImage image = createCaptchaImage(captchaCode);
        
        // 转换为Base64
        String imageBase64 = convertToBase64(image);
        
        return new CaptchaDTO(captchaId, imageBase64);
    }
    
    @Override
    public boolean validateCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) {
            return false;
        }
        
        String key = CAPTCHA_PREFIX + captchaId;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            return false; // 验证码不存在或已过期
        }
        
        // 验证后删除，防止重复使用
        redisTemplate.delete(key);
        
        return storedCode.equalsIgnoreCase(userInput); // 不区分大小写
    }
    
    /**
     * 生成随机验证码
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * 创建验证码图片
     */
    private BufferedImage createCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // 绘制干扰线
        Random random = new Random();
        g.setColor(new Color(220, 220, 220));
        for (int i = 0; i < 10; i++) {
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }
        
        // 绘制验证码
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            // 随机旋转
            double angle = (random.nextInt(60) - 30) * Math.PI / 180;
            g.rotate(angle, 25 + i * 20, 25);
            g.drawString(String.valueOf(code.charAt(i)), 25 + i * 20, 25);
            g.rotate(-angle, 25 + i * 20, 25);
        }
        
        g.dispose();
        return image;
    }
    
    /**
     * 将图片转换为Base64编码
     */
    private String convertToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("验证码图片转换失败", e);
        }
    }
} 