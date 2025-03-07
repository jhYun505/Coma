package com.coma.coma.post.controller;

import com.coma.coma.board.entity.Board;
import com.coma.coma.board.service.BoardService;
import com.coma.coma.comments.Dto.CommentDto;
import com.coma.coma.comments.Entity.Comment;
import com.coma.coma.comments.Service.CommentService;
import com.coma.coma.post.dto.PostResponseDto;
import com.coma.coma.post.service.PostService;
import com.coma.coma.security.CustomUserDetails;
import com.coma.coma.users.dto.UserResponseDto;
import com.coma.coma.users.service.UserService;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;


@Controller
@RequestMapping("/page/posts")
public class PostController {

    private final PostService postService;
    private final BoardService boardService;
    private final CommentService commentService;
    private final UserService userService;

    public PostController(PostService postService, BoardService boardService, CommentService commentService, UserService userService) {
        this.postService = postService;
        this.boardService = boardService;
        this.commentService = commentService;
        this.userService = userService;
    }

    // Authentication 통합해서 처리
    @ModelAttribute
    public void addUserInfoAttributes(@Nullable @AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        boolean isAuthenticated = customUserDetails != null;
        model.addAttribute("isAuthenticated", isAuthenticated);     // 로그인 여부 확인

        // 로그인이 된 경우
        if(isAuthenticated) {
            model.addAttribute("userId", customUserDetails.getUserId());
            // 관리자 계정 확인
            model.addAttribute("isAdmin", customUserDetails.getGroupId() == 1);
        } else {
            // 비로그인 사용자는 관리자 계정이 아님
            model.addAttribute("isAdmin", false);
        }
    }

    // 게시물 목록 페이지 - 페이지네이션 적용
    @GetMapping("/list/{boardId}")
    public String getPosts(@PathVariable("boardId") Long boardId,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           Model model) {
        Page<PostResponseDto> posts;
        Pageable pageable = PageRequest.of(page - 1, size); // 페이지 번호는 0부터 시작하므로 -1


        if (keyword != null && !keyword.isEmpty()) {
            posts = postService.findByKeywordWithPagination(boardId, keyword, pageable);
        } else {
            posts = postService.getPostsWithPagination(boardId, pageable);
        }

        // 보여줄 페이지 계산
        int totalPages = posts.getTotalPages();
        int currentPage = posts.getNumber() + 1;        // 페이지 번호를 1부터 시작하게
        int startPage = Math.max(1, (currentPage - 1) / 10 * 10 + 1);
        int endPage = Math.min(totalPages, startPage + 9);

        Board board = boardService.findOne(boardId);
        model.addAttribute("posts", posts);
        model.addAttribute("board", board);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        return "board/board";
    }

    // 게시글 상세 페이지
    @GetMapping("/{postId}")
    public String getPost(@PathVariable("postId") Integer postId,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          Model model) {

        PostResponseDto postResponseDto = postService.findPost(postId);
        // 글 작성자의 정보를 받아와 저장
        UserResponseDto authorInfo = userService.getUserByUserId(postResponseDto.getUserId());

        Integer loggedInUserId = (Integer) model.getAttribute("userId");
        Integer loggedInUserGroupId = (Integer) model.getAttribute("groupId");
        boolean isAuthor = loggedInUserId != null && loggedInUserId.equals(authorInfo.getUserId());


        //List<Comment> comments = commentService.getCommentsByPostId(postId);
        Page<Comment> comments;
        Pageable pageable = PageRequest.of(page - 1, size);
        comments = commentService.getCommentsPageByPostId(postId, pageable);

        int totalPages = comments.getTotalPages();
        int currentPage = comments.getNumber() + 1;
        int startPage = Math.max(1, (currentPage - 1) / 10 * 10 + 1);
        int endPage = Math.min(totalPages, startPage + 9);

        model.addAttribute("post", postResponseDto);
        model.addAttribute("comments", comments);
        model.addAttribute("isAuthor", isAuthor);
        model.addAttribute("authorInfo", authorInfo);       // 글 작성자 정보 전달
        model.addAttribute("loggedInUserId", loggedInUserId); // 현재 로그인 한 유저 User_Id 전달
        model.addAttribute("loggedInUserGroupId",loggedInUserGroupId);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", currentPage);
        return "post/post";
    }

    // 새 게시물 작성 페이지 이동
    @GetMapping("/new/{boardId}")
    public String newPostPage(@PathVariable("boardId") Long boardId, Model model) {
        model.addAttribute("boardId", boardId);
        return "post/createPost";
    }


    // 게시물 수정 페이지 이동
    @GetMapping("/edit/{postId}")
    public String editPostPage(@PathVariable("postId") Integer postId, Model model) {
        PostResponseDto post = postService.findPost(postId);
        // 글에 이미지 파일이 있는 경우
        if(post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            String[] tokens= post.getImageUrl().split("/");
            String fileName = tokens[tokens.length - 1];
            model.addAttribute("filename", fileName);
            System.out.println(fileName);
        }
        model.addAttribute("post", post);
        return "post/editPost";
    }

}
