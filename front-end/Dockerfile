FROM node:22-alpine
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
ARG ZELO_API_URL=http://backend:8080
ENV ZELO_API_URL=${ZELO_API_URL}
RUN npm run build
EXPOSE 3000
CMD ["npm", "run", "start"]

