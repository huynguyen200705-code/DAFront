# ============================================================
# Dockerfile — Web API Quản Lý Nhà Hàng & POS
# Tác giả: 0859 - Nguyễn Quốc Huy
# Tương thích: Render.com / Railway.app / Docker local
# ============================================================

# Stage 1: Build & Publish
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Copy project file và restore dependencies
COPY ["0859_NguyenQuocHuy_WebAPI/0859_NguyenQuocHuy_WebAPI.csproj", "0859_NguyenQuocHuy_WebAPI/"]
RUN dotnet restore "0859_NguyenQuocHuy_WebAPI/0859_NguyenQuocHuy_WebAPI.csproj"

# Copy toàn bộ mã nguồn và publish release
COPY . .
WORKDIR "/src/0859_NguyenQuocHuy_WebAPI"
RUN dotnet publish "0859_NguyenQuocHuy_WebAPI.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Stage 2: Runtime image nhẹ
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS final
WORKDIR /app

# Render.com / Railway tự inject $PORT — dùng http://+:$PORT
# Local Docker: mặc định port 8080
ENV ASPNETCORE_ENVIRONMENT=Production
ENV ASPNETCORE_URLS=http://+:${PORT:-8080}
ENV UseSqlite=true

EXPOSE 8080

COPY --from=build /app/publish .

ENTRYPOINT ["dotnet", "_0859_NguyenQuocHuy_WebAPI.dll"]
