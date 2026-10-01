package com.personal.cinemabooking.controllers;

import com.personal.cinemabooking.dto.MovieDTO;
import com.personal.cinemabooking.dto.MovieRequest;
import com.personal.cinemabooking.entities.Movie;
import com.personal.cinemabooking.core.exceptions.ValidationException;
import com.personal.cinemabooking.services.MovieService;
import com.personal.cinemabooking.integration.media.S3StorageService;
import com.personal.cinemabooking.core.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/movies")
@Slf4j
public class MovieController {

    private final MovieService movieService;
    private final S3StorageService s3StorageService;
    @GetMapping
    public BaseResponse getMovies(
            @PageableDefault(page = 0, size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String year) {
        
        Integer releaseYear = null;
        if (year != null && !year.isEmpty()) {
            try {
                releaseYear = Integer.parseInt(year);
            } catch (NumberFormatException ignored) {}
        }
        
        Page<MovieDTO> movies;
        if ((genre != null && !genre.isEmpty()) || releaseYear != null) {
            movies = movieService.findMoviesWithFilters(search, genre, releaseYear, pageable);
        } else if (search != null && !search.isEmpty()) {
            movies = movieService.findByTitleOrGenreContainingIgnoreCase(search, pageable);
        } else {
            movies = movieService.findAllWithReviews(pageable);
        }

        return BaseResponse.success("movie.retrieved.success", movies);
    }

    @GetMapping("/{id}")
    public BaseResponse getMovieById(@PathVariable Long id) {
        return BaseResponse.success("movie.retrieved.success", movieService.getMovieById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public BaseResponse addMovie(@Valid @RequestBody MovieRequest movieRequest) {
        Movie movie = new Movie();
        movie.setTitle(movieRequest.getTitle());
        movie.setGenre(movieRequest.getGenre());
        movie.setReleaseYear(movieRequest.getReleaseYear());
        movie.setDescription(movieRequest.getDescription());
        movie.setPosterImageUrl(movieRequest.getPosterImageUrl());

        MovieDTO savedMovie = movieService.addMovie(movie);
        return BaseResponse.success("movie.created.success", savedMovie);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public BaseResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest movieRequest) {
        Movie movieDetails = new Movie();
        movieDetails.setTitle(movieRequest.getTitle());
        movieDetails.setGenre(movieRequest.getGenre());
        movieDetails.setReleaseYear(movieRequest.getReleaseYear());
        movieDetails.setDescription(movieRequest.getDescription());
        movieDetails.setPosterImageUrl(movieRequest.getPosterImageUrl());

        MovieDTO updatedMovie = movieService.updateMovie(id, movieDetails);
        return BaseResponse.success("movie.updated.success", updatedMovie);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public BaseResponse deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return BaseResponse.success("movie.deleted.success");
    }

    @PostMapping("/{id}/poster")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public BaseResponse uploadMoviePoster(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return BaseResponse.fail("poster.upload.empty");
        }

        MovieDTO movie = movieService.getMovieById(id);
        Map<String, Object> uploadResult = s3StorageService.uploadImage(file);
        String posterUrl = (String) uploadResult.get("secure_url");

        Movie movieDetails = new Movie();
        movieDetails.setTitle(movie.getTitle());
        movieDetails.setGenre(movie.getGenre());
        movieDetails.setReleaseYear(movie.getReleaseYear());
        movieDetails.setDescription(movie.getDescription());
        movieDetails.setPosterImageUrl(posterUrl);

        MovieDTO updatedMovie = movieService.updateMovie(id, movieDetails);
        uploadResult.put("movie", updatedMovie);

        return BaseResponse.success("poster.upload.success", uploadResult);
    }
}